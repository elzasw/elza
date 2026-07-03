#!/usr/bin/env node
/**
 * Verify target locale catalogs are in sync with the source (cs) and fully translated.
 *
 * Read-only. Intended for CI / pre-commit. The source catalog (cs) must already
 * be present — run `locale:extract` first (the source is ephemeral). For every
 * requested target locale, fails (exit 1) when it:
 *   - is missing keys present in the source (drift — run `locale:merge`),
 *   - has stale keys absent from the source (drift — run `locale:merge`),
 *   - has keys marked `translated: false` (untranslated),
 *   - has keys whose `srcHash` no longer matches the source (outdated — the
 *     source text changed since translation).
 *
 * The source locale itself is skipped if passed (it cannot be checked against itself).
 *
 * Usage:
 *   node scripts/locale-check.ts [--source <loc>] <locale...>
 *   node scripts/locale-check.ts --source cs en
 */
import { readFileSync, existsSync } from "node:fs";
import { checkCatalog, isClean } from "./locale-check-core.ts";
import { hashMessage, type Catalog } from "./locale-merge-core.ts";

const TRANSLATED_DIR = "lang/translated";
const MAX_LIST = 20;

const args = process.argv.slice(2);
let sourceLocale = "cs";
const locales: string[] = [];
for (let i = 0; i < args.length; i++) {
  if (args[i] === "--source") {
    sourceLocale = args[++i];
  } else {
    locales.push(args[i]);
  }
}

if (locales.length === 0) {
  console.error("Usage: node scripts/locale-check.ts [--source <loc>] <locale...>  (e.g. --source cs en)");
  process.exit(1);
}

function readJson(path: string): Catalog {
  return JSON.parse(readFileSync(path, "utf8"));
}

const sourcePath = `${TRANSLATED_DIR}/${sourceLocale}.json`;
if (!existsSync(sourcePath)) {
  console.error(`Source catalog not found: ${sourcePath}. Run \`npm run locale:extract\` first.`);
  process.exit(1);
}

const source = readJson(sourcePath);
const sourceHashes = new Map<string, string>(
  Object.keys(source).map((id) => [id, hashMessage(source[id].defaultMessage)]),
);

// Print at most MAX_LIST ids, then a count of the rest, so CI logs stay readable.
function listIds(label: string, ids: string[]): void {
  if (ids.length === 0) {
    return;
  }
  console.error(`  ${label} (${ids.length}):`);
  ids.slice(0, MAX_LIST).forEach((id) => console.error(`    ${id}`));
  if (ids.length > MAX_LIST) {
    console.error(`    …and ${ids.length - MAX_LIST} more`);
  }
}

let failed = false;

for (const locale of locales) {
  if (locale === sourceLocale) {
    continue;
  }

  const target = `${TRANSLATED_DIR}/${locale}.json`;
  if (!existsSync(target)) {
    console.error(`✗ ${locale}: catalog missing (${target}). Run \`npm run locale:merge\`.`);
    failed = true;
    continue;
  }

  const catalog = readJson(target);
  const result = checkCatalog(sourceHashes, catalog);

  if (isClean(result)) {
    console.log(`✓ ${locale}: ${Object.keys(catalog).length} keys, in sync and fully translated.`);
    continue;
  }

  failed = true;
  console.error(`✗ ${locale}:`);
  listIds("missing (run locale:merge)", result.missing);
  listIds("stale (run locale:merge)", result.stale);
  listIds("untranslated (translated: false)", result.untranslated);
  listIds("outdated (source changed — re-merge + re-translate)", result.outdated);
}

process.exit(failed ? 1 : 0);
