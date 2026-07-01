import { createHash } from "node:crypto";

export interface Descriptor {
  defaultMessage: string;
  /** Present on target locales only. Absent = untranslated / needs work. */
  translated?: boolean;
  /**
   * Target locales only: hash of the source (cs) defaultMessage this entry was
   * translated against. Lets a later merge/check detect that the source text
   * changed (hash drift) and the translation is now stale. Never set on the
   * source locale.
   */
  srcHash?: string;
}

export type Catalog = Record<string, Descriptor>;

export interface MergeResult {
  merged: Catalog;
  added: string[];
  stale: string[];
  /** Target keys re-flagged because the source text changed since translation. */
  outdated: string[];
}

export interface MergeOptions {
  /**
   * Whether this locale is the source language (its defaultMessage is the
   * authoritative text). The source is seeded without a `translated` flag or
   * `srcHash` — it cannot be stale against itself. Every other locale's new
   * keys are flagged untranslated and carry a srcHash.
   */
  isSource?: boolean;
}

/** Stable short hash of a message, used to detect source-text drift. */
export function hashMessage(message: string): string {
  return createHash("sha256").update(message).digest("base64url").slice(0, 12);
}

/**
 * Merge an extracted source catalog into one locale's current catalog.
 *
 * Source locale: keys seeded from the extracted defaultMessage, no flag/hash.
 *
 * Target locale: existing translations are kept, but if the source text changed
 * since they were translated (stored `srcHash` != current source hash) the entry
 * is re-flagged `translated: false` and its srcHash bumped to the current source.
 * New keys are seeded with the source defaultMessage, `translated: false`, and
 * the current srcHash. Keys absent from the source are dropped. Result follows
 * source key order.
 *
 * @param extracted - source catalog (descriptor format)
 * @param current - the locale's existing catalog (empty object if new)
 * @param options - merge options (see MergeOptions)
 */
export function mergeCatalog(extracted: Catalog, current: Catalog, options: MergeOptions = {}): MergeResult {
  const merged: Catalog = {};
  const added: string[] = [];
  const outdated: string[] = [];

  for (const id of Object.keys(extracted)) {
    const srcHash = hashMessage(extracted[id].defaultMessage);

    if (options.isSource) {
      merged[id] = { defaultMessage: extracted[id].defaultMessage };
      if (!(id in current)) {
        added.push(id);
      }
      continue;
    }

    if (id in current) {
      const entry = current[id];
      if (entry.srcHash !== srcHash) {
        merged[id] = { ...entry, translated: false, srcHash };
        outdated.push(id);
      } else {
        merged[id] = entry;
      }
    } else {
      merged[id] = { defaultMessage: extracted[id].defaultMessage, translated: false, srcHash };
      added.push(id);
    }
  }

  const stale = Object.keys(current).filter((id) => !(id in extracted));

  return { merged, added, stale, outdated };
}
