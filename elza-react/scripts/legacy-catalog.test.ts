// @vitest-environment node
import { describe, expect, it } from "vitest";
import { readFileSync, readdirSync } from "node:fs";
import { extname, join, sep } from "node:path";

import {
    collectUsed,
    countDynamicCalls,
    DYNAMIC_CALL_FILES,
    DYNAMIC_KEYS,
} from "./legacy-catalog-core.ts";

/**
 * Pojistka nad oříznutým legacy katalogem.
 *
 * `messages_cs.js` je ořezaný na klíče, které kód ještě čte (viz
 * `scripts/legacy-catalog-prune.ts`). Helper `i18n()` na chybějící klíč
 * nespadne - vypíše `[klíč]` přímo do UI. Bez téhle kontroly by se takový
 * překlep projevil až u uživatele.
 */

const CATALOG_PATH = "public/static/res/js/messages_cs.js";
const SOURCE_DIR = "src";
const EXTENSIONS = new Set([".js", ".jsx", ".ts", ".tsx"]);
const HELPER = join("src", "components", "i18n.jsx");

function sourceFiles(dir: string): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const path = join(dir, entry.name);
        if (entry.isDirectory()) return sourceFiles(path);
        if (path === HELPER) return [];
        return EXTENSIONS.has(extname(entry.name)) ? [path] : [];
    });
}

const catalog = new Function(
    `${readFileSync(CATALOG_PATH, "utf8")}; return messages;`,
)() as Record<string, string>;

const files = sourceFiles(SOURCE_DIR);

describe("messages_cs.js", () => {
    it("obsahuje každý klíč, na který se kód odkazuje literálem", () => {
        const used = collectUsed(files.map((path) => readFileSync(path, "utf8")));
        const missing = [...used].filter((key) => catalog[key] === undefined).sort();
        expect(missing).toEqual([]);
    });

    it("obsahuje klíče skládané za běhu", () => {
        const missing = DYNAMIC_KEYS.filter((key) => catalog[key] === undefined).sort();
        expect(missing).toEqual([]);
    });

    /**
     * Skládaný klíč se ve zdroji staticky nenajde, takže by ho prune zahodil.
     * Proto je každé takové místo vypsané v `DYNAMIC_CALL_FILES` a jeho klíče
     * v `DYNAMIC_KEYS`. Kdyby skládané volání přibylo jinde, spadne tenhle test.
     */
    it("nemá skládané volání i18n() mimo evidované soubory", () => {
        const expected = DYNAMIC_CALL_FILES.map((path) => path.split("/").join(sep));
        const offenders = files
            .filter((path) => !expected.includes(path))
            .filter((path) => countDynamicCalls(readFileSync(path, "utf8")) > 0)
            .map((path) => path.split(sep).join("/"))
            .sort();
        expect(offenders).toEqual([]);
    });
});
