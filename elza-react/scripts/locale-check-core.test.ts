import { describe, it, expect } from "vitest";
import { checkCatalog, isClean } from "./locale-check-core.ts";

const sourceHashes = new Map([
  ["a", "hashA"],
  ["b", "hashB"],
]);

describe("checkCatalog", () => {
  it("reports a clean catalog with no findings", () => {
    const catalog = {
      a: { defaultMessage: "Hello", srcHash: "hashA" },
      b: { defaultMessage: "World", srcHash: "hashB" },
    };
    const result = checkCatalog(sourceHashes, catalog);
    expect(result).toEqual({ missing: [], stale: [], untranslated: [], outdated: [] });
    expect(isClean(result)).toBe(true);
  });

  it("detects keys missing from the catalog", () => {
    const catalog = { a: { defaultMessage: "Hello", srcHash: "hashA" } };
    const result = checkCatalog(sourceHashes, catalog);
    expect(result.missing).toEqual(["b"]);
    expect(isClean(result)).toBe(false);
  });

  it("detects stale keys absent from the source", () => {
    const catalog = {
      a: { defaultMessage: "Hello", srcHash: "hashA" },
      b: { defaultMessage: "World", srcHash: "hashB" },
      x: { defaultMessage: "Old", srcHash: "z" },
    };
    const result = checkCatalog(sourceHashes, catalog);
    expect(result.stale).toEqual(["x"]);
    expect(isClean(result)).toBe(false);
  });

  it("detects untranslated keys via translated:false", () => {
    const catalog = {
      a: { defaultMessage: "Hello", srcHash: "hashA" },
      b: { defaultMessage: "Svět", translated: false, srcHash: "hashB" },
    };
    const result = checkCatalog(sourceHashes, catalog);
    expect(result.untranslated).toEqual(["b"]);
    expect(isClean(result)).toBe(false);
  });

  it("detects outdated keys whose srcHash no longer matches the source", () => {
    const catalog = {
      a: { defaultMessage: "Hello", srcHash: "STALE" },
      b: { defaultMessage: "World", srcHash: "hashB" },
    };
    const result = checkCatalog(sourceHashes, catalog);
    expect(result.outdated).toEqual(["a"]);
    expect(isClean(result)).toBe(false);
  });

  it("does not flag a key without srcHash as outdated (only missing/untranslated logic applies)", () => {
    const catalog = {
      a: { defaultMessage: "Hello" },
      b: { defaultMessage: "World", srcHash: "hashB" },
    };
    const result = checkCatalog(sourceHashes, catalog);
    expect(result.outdated).toEqual([]);
  });
});
