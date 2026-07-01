#!/usr/bin/env node
/**
 * Merge extracted messages into translated locale catalogs.
 *
 * Run after `formatjs extract`. For each requested locale it keeps the existing
 * human translations, adds any newly extracted keys (seeded with the extracted
 * defaultMessage and marked `translated: false`), and drops keys that no longer
 * exist in the source.
 *
 * The `translated: false` marker lets `locale:check` flag untranslated strings.
 * It is stripped by `formatjs compile-folder`, so it never reaches the runtime
 * catalog or the UI. A human flips it to `true` (or removes it) once the string
 * is translated.
 *
 * The source locale (`--source <loc>`) holds the authoritative text; its new
 * keys are not flagged `translated: false`. Other locales' new keys are.
 *
 * Usage:
 *   node scripts/locale-merge.ts [--source <loc>] <locale...>
 *   node scripts/locale-merge.ts --source cs cs en
 *
 * Paths are fixed to the project layout:
 *   source:  lang/extracted/cs.json   (descriptor format {id: {defaultMessage}})
 *   targets: lang/translated/<locale>.json
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname } from "node:path";
import { mergeCatalog, type Catalog } from "./locale-merge-core.ts";

const TRANSLATED_DIR = "lang/translated";
// The source catalog (cs) is produced ephemerally by `locale:extract` into the translated dir.
const SOURCE = `${TRANSLATED_DIR}/cs.json`;

const args = process.argv.slice(2);
let sourceLocale: string | undefined;
const locales: string[] = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] === "--source") {
    sourceLocale = args[++i];
  } else {
    locales.push(args[i]);
  }
}

if (locales.length === 0) {
  console.error("Usage: node scripts/locale-merge.ts [--source <loc>] <locale...>  (e.g. --source cs cs en)");
  process.exit(1);
}

function readJson(path: string): Catalog {
  return JSON.parse(readFileSync(path, "utf8"));
}

if (!existsSync(SOURCE)) {
  console.error(`Source catalog not found: ${SOURCE}. Run \`npm run locale:extract\` first.`);
  process.exit(1);
}

const extracted = readJson(SOURCE);
const keyCount = Object.keys(extracted).length;

let hadChanges = false;

for (const locale of locales) {
  const target = `${TRANSLATED_DIR}/${locale}.json`;
  const existing = existsSync(target) ? readJson(target) : null;
  const created = existing === null;

  const { merged, added, stale, outdated } = mergeCatalog(extracted, existing ?? {}, {
    isSource: locale === sourceLocale,
  });

  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, JSON.stringify(merged, null, 2) + "\n");

  if (created || added.length > 0 || stale.length > 0 || outdated.length > 0) {
    hadChanges = true;
  }
  const status = created ? "created" : "updated";
  console.log(
    `${locale}: ${status} — ${keyCount} keys (added ${added.length}, dropped ${stale.length}, outdated ${outdated.length})`,
  );
  stale.forEach((id) => console.log(`  - dropped stale: ${id}`));
  outdated.forEach((id) => console.log(`  ~ source changed, re-flag: ${id}`));
}

if (!hadChanges) {
  console.log("All locales already in sync.");
}
