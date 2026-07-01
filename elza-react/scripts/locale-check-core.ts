import type { Catalog } from "./locale-merge-core.ts";

export interface CheckResult {
  missing: string[];
  stale: string[];
  untranslated: string[];
  outdated: string[];
}

/**
 * Compare one target locale's catalog against the source.
 *
 * missing: source ids absent from the catalog (drift — merge not run);
 * stale: catalog ids absent from the source (drift — merge not run);
 * untranslated: ids marked `translated: false`;
 * outdated: ids whose stored `srcHash` no longer matches the current source
 *   hash — the source text changed since translation (merge not run, or the
 *   translation was never refreshed).
 *
 * @param sourceHashes - id → current hash of the source (cs) defaultMessage
 * @param catalog - the target locale's catalog
 */
export function checkCatalog(sourceHashes: Map<string, string>, catalog: Catalog): CheckResult {
  const ids = Object.keys(catalog);
  const idSet = new Set(ids);

  const missing = Array.from(sourceHashes.keys()).filter((id) => !idSet.has(id));
  const stale = ids.filter((id) => !sourceHashes.has(id));
  const untranslated = ids.filter((id) => catalog[id]?.translated === false);
  const outdated = ids.filter((id) => {
    const entry = catalog[id];
    // Only meaningful for keys that exist in the source and carry a srcHash.
    return entry.srcHash !== undefined && sourceHashes.has(id) && sourceHashes.get(id) !== entry.srcHash;
  });

  return { missing, stale, untranslated, outdated };
}

/** True when the catalog is in sync and fully translated. */
export function isClean(result: CheckResult): boolean {
  return (
    result.missing.length === 0 &&
    result.stale.length === 0 &&
    result.untranslated.length === 0 &&
    result.outdated.length === 0
  );
}
