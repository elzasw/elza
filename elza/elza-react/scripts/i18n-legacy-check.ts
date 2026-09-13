#!/usr/bin/env node
/**
 * Pojistka proti novým voláním legacy helperu `i18n()`.
 *
 * Stejný princip jako `ts-strict-check.ts`: opt-out, ne opt-in. Prochází se
 * všechny zdrojové soubory a známý dluh je vypsaný v `i18n-legacy-baseline.json`.
 * Nový soubor tedy nemá baseline záznam a jakékoli legacy volání v něm rovnou
 * propadne. Baseline smí jen zmenšovat - `--update` odmítne zapsat větší počet.
 *
 * Počty jsou **per soubor**, ne jeden součet: jinak by migrace pěti volání
 * v souboru A zamaskovala pět nově přidaných v souboru B.
 *
 * Použití:
 *   node scripts/i18n-legacy-check.ts             porovnat s baseline
 *   node scripts/i18n-legacy-check.ts --update    přepsat baseline (jen dolů)
 */
import { existsSync, readFileSync, writeFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, sep } from "node:path";

import {
    compareLegacy,
    countCalls,
    countTotal,
    type LegacyBaseline,
} from "./i18n-legacy-core.ts";

const BASELINE_PATH = "i18n-legacy-baseline.json";
const ROOT = "src";
const EXTENSIONS = [".ts", ".tsx", ".js", ".jsx"];

const update = process.argv.includes("--update");

/** Soubor s helperem samotným obsahuje ukázky v dokumentačním komentáři. */
const IGNORED = new Set(["src/components/i18n.jsx"]);

const isSource = (file: string): boolean =>
    EXTENSIONS.some((ext) => file.endsWith(ext)) &&
    !file.endsWith(".d.ts") &&
    !/\.(test|spec)\.[cm]?[jt]sx?$/.test(file);

function collect(dir: string, out: string[] = []): string[] {
    for (const entry of readdirSync(dir)) {
        const path = join(dir, entry);
        if (statSync(path).isDirectory()) {
            collect(path, out);
        } else if (isSource(path)) {
            out.push(path);
        }
    }
    return out;
}

function scan(): LegacyBaseline {
    const counts: LegacyBaseline = {};
    for (const path of collect(ROOT)) {
        const file = relative(".", path).split(sep).join("/");
        if (IGNORED.has(file)) {
            continue;
        }
        const count = countCalls(readFileSync(path, "utf8"));
        if (count > 0) {
            counts[file] = count;
        }
    }
    return Object.fromEntries(Object.entries(counts).sort(([a], [b]) => a.localeCompare(b)));
}

const counts = scan();

if (update) {
    const previous: LegacyBaseline = existsSync(BASELINE_PATH)
        ? JSON.parse(readFileSync(BASELINE_PATH, "utf8"))
        : {};
    const before = countTotal(previous);
    const after = countTotal(counts);
    if (existsSync(BASELINE_PATH) && after > before) {
        console.error(`✗ baseline se nesmí zvětšovat: ${before} -> ${after}.`);
        console.error("  Nová volání i18n() převeď na react-intl (viz .claude/rules/i18n.md).");
        process.exit(1);
    }
    writeFileSync(BASELINE_PATH, JSON.stringify(counts, null, 2) + "\n", "utf8");
    console.log(
        `✓ baseline zapsána: ${before} -> ${after} volání v ${Object.keys(counts).length} souborech.`,
    );
    process.exit(0);
}

if (!existsSync(BASELINE_PATH)) {
    console.error(`✗ chybí ${BASELINE_PATH}. Vytvoř ho: npm run i18n:legacy-baseline`);
    process.exit(1);
}

const baseline: LegacyBaseline = JSON.parse(readFileSync(BASELINE_PATH, "utf8"));
const { added, improved, total } = compareLegacy(counts, baseline);

if (added.length > 0) {
    console.error(`✗ přibyla volání legacy i18n() v ${added.length} soubor(ech):\n`);
    for (const entry of added) {
        console.error(`  ${entry.file}: ${entry.baseline} -> ${entry.actual}`);
    }
    console.error("\nNové texty patří do react-intl (defineMessages + FormattedMessage).");
    console.error("Viz .claude/rules/i18n.md.");
    process.exit(1);
}

if (improved.length > 0) {
    const removed = improved.reduce((sum, e) => sum + (e.baseline - e.actual), 0);
    console.log(`✓ žádná nová legacy volání. Ubylo ${removed} v ${improved.length} soubor(ech) —`);
    console.log("  zmenši baseline: npm run i18n:legacy-baseline");
} else {
    console.log(`✓ žádná nová legacy volání (${total} známých, všechna v baseline).`);
}
