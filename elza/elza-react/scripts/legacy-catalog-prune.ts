/**
 * Ořeže `public/static/res/js/messages_cs.js` na klíče, které kód ještě
 * potřebuje.
 *
 *     node scripts/legacy-catalog-prune.ts            # jen vypíše, co by udělal
 *     node scripts/legacy-catalog-prune.ts --write    # zapíše
 *
 * Katalog se načítá `<script>` tagem v `index.html`, tedy celý při každém
 * načtení stránky. Po migraci na react-intl z něj zbývá potřeba jen zlomku.
 *
 * Skript je záměrně spustitelný opakovaně: po každé další dávce migrace se
 * pustí znovu a katalog se zmenší. Až v `src/` nezbude žádné `i18n(`, vyjde
 * prázdný a celý soubor i `<script>` tag mohou zmizet.
 */
import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { extname, join, sep } from "node:path";

import { collectUsed, DYNAMIC_KEYS } from "./legacy-catalog-core.ts";

const CATALOG_PATH = "public/static/res/js/messages_cs.js";
const SOURCE_DIR = "src";
const EXTENSIONS = new Set([".js", ".jsx", ".ts", ".tsx"]);
/** Vlastní helper - jeho dokumentační komentář obsahuje ukázkové klíče. */
const HELPER = join("src", "components", "i18n.jsx");

function sources(dir: string): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const path = join(dir, entry.name);
        if (entry.isDirectory()) return sources(path);
        if (path === HELPER) return [];
        return EXTENSIONS.has(extname(entry.name)) ? [path] : [];
    });
}

function readCatalog(path: string): Record<string, string> {
    const source = readFileSync(path, "utf8");
    return new Function(`${source}; return messages;`)() as Record<string, string>;
}

function render(catalog: Record<string, string>, keys: string[]): string {
    const entries = keys
        .map((key) => `    ${JSON.stringify(key)}: ${JSON.stringify(catalog[key])},`)
        .join("\n");
    return `/**
 * Zbytek legacy katalogu - klíče, které ještě čte helper \`components/i18n\`.
 *
 * Soubor se načítá <script> tagem v index.html a NENÍ součástí react-intl
 * pipeline: nedá se přeložit do jiného jazyka. Všechno ostatní už je
 * v lang/translated/.
 *
 * Generováno: node scripts/legacy-catalog-prune.ts --write
 * Needitovat ručně - při další dávce migrace se přegeneruje.
 */
var messages = {
${entries}
};
`;
}

const write = process.argv.includes("--write");
const catalog = readCatalog(CATALOG_PATH);
const used = collectUsed(sources(SOURCE_DIR).map((path) => readFileSync(path, "utf8")));

const needed = new Set([...used, ...DYNAMIC_KEYS]);
const keep = Object.keys(catalog).filter((key) => needed.has(key)).sort();
const drop = Object.keys(catalog).length - keep.length;

/** Klíč, na který se kód odkazuje, ale v katalogu není - i18n() vypíše `[klíč]`. */
const missing = [...needed].filter((key) => catalog[key] === undefined).sort();

console.log(`${CATALOG_PATH}: ${Object.keys(catalog).length} klíčů`);
console.log(`  ponechat: ${keep.length}  (z toho ${DYNAMIC_KEYS.length} skládaných za běhu)`);
console.log(`  zahodit:  ${drop}`);
if (missing.length > 0) {
    console.log(`\n  POZOR - kód se odkazuje na ${missing.length} klíč(ů), které v katalogu nejsou:`);
    for (const key of missing) console.log(`    ${key}`);
}

if (!write) {
    console.log("\n(nic nezapsáno, spusť s --write)");
    process.exit(0);
}

writeFileSync(CATALOG_PATH, render(catalog, keep));
console.log(`\nzapsáno: ${CATALOG_PATH.split("/").join(sep)}`);
