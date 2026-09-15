// @vitest-environment node
// esbuild potrebuje nodovy TextEncoder; v jsdom prostredi odmitne nastartovat.
import { describe, expect, it } from "vitest";
import { readdirSync, readFileSync } from "node:fs";
import { extname, join } from "node:path";
import { transform } from "esbuild";

/**
 * Syntaktická pojistka nad celým `src/`.
 *
 * Proč vůbec existuje: `tsc --noEmit` **`.js` a `.jsx` soubory vůbec nečte**
 * (tsconfig nemá `allowJs`), a vitest přeloží jen to, co si nějaký test
 * naimportuje. Necelá polovina zdrojů je pořád `.jsx`, takže v nich může
 * projít i obyčejná syntaktická chyba - zachytí ji až `vite build`, který
 * běží jen v CI na konci mavenu.
 *
 * Přesně to se stalo u `title=<FormattedMessage ... />` (chybějící složené
 * závorky kolem hodnoty atributu): tsc mlčel, testy mlčely, spadl až release.
 *
 * Test proto přežene každý zdroj stejným parserem, jaký používá vite. Je to
 * jen parsování, ne typová kontrola - běží v jednotkách sekund.
 */

const SOURCE_DIR = "src";
const EXTENSIONS = new Set([".js", ".jsx", ".ts", ".tsx"]);

/** esbuild loader podle přípony; `.js`/`.jsx` v tomhle repu obsahují JSX. */
const LOADERS = {
    ".js": "jsx",
    ".jsx": "jsx",
    ".ts": "ts",
    ".tsx": "tsx",
} as const;

function collectSources(dir: string): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const path = join(dir, entry.name);
        if (entry.isDirectory()) return collectSources(path);
        return EXTENSIONS.has(extname(entry.name)) ? [path.replace(/\\/g, "/")] : [];
    });
}

describe("src/", () => {
    it("je celé syntakticky platné", async () => {
        const files = collectSources(SOURCE_DIR);
        expect(files.length).toBeGreaterThan(100);

        const failures: string[] = [];
        await Promise.all(
            files.map(async (file) => {
                const loader = LOADERS[extname(file) as keyof typeof LOADERS];
                try {
                    await transform(readFileSync(file, "utf8"), { loader, sourcefile: file });
                } catch (error) {
                    const messages = (error as { errors?: Array<{ text: string; location?: { line: number } }> }).errors;
                    const detail = messages?.map((e) => `${file}:${e.location?.line}: ${e.text}`).join("\n");
                    failures.push(detail ?? `${file}: ${String(error)}`);
                }
            }),
        );

        expect(failures).toEqual([]);
    });
});
