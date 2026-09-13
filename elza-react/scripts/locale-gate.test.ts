import { describe, it, expect } from "vitest";
import { existsSync, readFileSync } from "node:fs";

import { checkCatalog } from "./locale-check-core.ts";
import { hashMessage, type Catalog } from "./locale-merge-core.ts";

/**
 * Překladová pojistka spuštěná jako běžný test.
 *
 * `locale:check` existuje jako samostatný skript, ale nikde se nespouští:
 * release lane v elza-build.git ho záměrně nemá (překlady nesmí blokovat
 * release). Jediná páka, která běží u vývojáře i v CI, je `npm test` - proto
 * je pojistka tady.
 *
 * Politika: **kde je český zdroj, musí být anglický překlad.** Pro další jazyky
 * (třeba budoucí částečnou němčinu) se to nevyžaduje - ty se sem prostě
 * nepřidají.
 */

const TRANSLATED_DIR = "lang/translated";
const SOURCE_LOCALE = "cs";
const REQUIRED_LOCALE = "en";

const sourcePath = `${TRANSLATED_DIR}/${SOURCE_LOCALE}.json`;
const requiredPath = `${TRANSLATED_DIR}/${REQUIRED_LOCALE}.json`;

const readCatalog = (path: string): Catalog => JSON.parse(readFileSync(path, "utf8"));

describe("lang/translated/en.json", () => {
    it("nemá nepřeložené klíče", () => {
        const catalog = readCatalog(requiredPath);
        const untranslated = Object.keys(catalog).filter((id) => catalog[id]?.translated === false);
        expect(untranslated).toEqual([]);
    });

    /**
     * `cs.json` je efemérní a gitignorovaný (generuje ho `locale:extract`), takže
     * na čerstvém checkoutu chybí. Plný sync-check proto běží jen když je k
     * dispozici; test výše naopak běží vždy, protože pokrývá právě ten případ,
     * kdy by uživatel v anglickém UI uviděl češtinu.
     */
    it.runIf(existsSync(sourcePath))("je v syncu se zdrojovým katalogem", () => {
        const source = readCatalog(sourcePath);
        const sourceHashes = new Map(
            Object.keys(source).map((id) => [id, hashMessage(source[id].defaultMessage)]),
        );

        expect(checkCatalog(sourceHashes, readCatalog(requiredPath))).toEqual({
            missing: [],
            stale: [],
            untranslated: [],
            outdated: [],
        });
    });

    /**
     * V ICU otevírá apostrof doslovný literál, takže `'{code}'` se vypíše jako
     * text `{code}` a hodnota se **nikdy nedosadí - bez jakékoli chyby**.
     * Legacy katalog tuhle pastí obsahuje na deseti místech; tenhle test hlídá,
     * aby se sem nedostala při další migraci.
     */
    it("nemá placeholder uzavřený v apostrofech", () => {
        const catalogs = [requiredPath, sourcePath].filter(existsSync);
        const offenders = catalogs.flatMap((path) =>
            Object.entries(readCatalog(path))
                .filter(([, entry]) => /'\{[^}]+\}'/.test(entry.defaultMessage))
                .map(([id]) => `${path}: ${id}`),
        );
        expect(offenders).toEqual([]);
    });
});
