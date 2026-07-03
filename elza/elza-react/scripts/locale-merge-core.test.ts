import { describe, it, expect } from "vitest";
import { mergeCatalog, hashMessage } from "./locale-merge-core.ts";

const extracted = {
  a: { defaultMessage: "Ahoj" },
  b: { defaultMessage: "Svět" },
};
const hashA = hashMessage("Ahoj");
const hashB = hashMessage("Svět");

describe("mergeCatalog — target locale", () => {
  it("keeps an up-to-date translation verbatim (srcHash matches current source)", () => {
    const current = {
      a: { defaultMessage: "Hello", srcHash: hashA },
      b: { defaultMessage: "World", srcHash: hashB },
    };
    const { merged, added, stale, outdated } = mergeCatalog(extracted, current);
    expect(merged.a).toEqual({ defaultMessage: "Hello", srcHash: hashA });
    expect(added).toEqual([]);
    expect(stale).toEqual([]);
    expect(outdated).toEqual([]);
  });

  it("seeds a new key: default + translated:false + current srcHash", () => {
    const current = { a: { defaultMessage: "Hello", srcHash: hashA } };
    const { merged, added } = mergeCatalog(extracted, current);
    expect(merged.b).toEqual({ defaultMessage: "Svět", translated: false, srcHash: hashB });
    expect(added).toEqual(["b"]);
  });

  it("creates every key for a brand-new locale, flagged + hashed", () => {
    const { merged, added } = mergeCatalog(extracted, {});
    expect(added).toEqual(["a", "b"]);
    expect(merged.a).toEqual({ defaultMessage: "Ahoj", translated: false, srcHash: hashA });
    expect(merged.b).toEqual({ defaultMessage: "Svět", translated: false, srcHash: hashB });
  });

  it("re-flags an entry as untranslated when the source text changed (srcHash drift)", () => {
    const current = { a: { defaultMessage: "Hello", srcHash: "OLDHASH" }, b: { defaultMessage: "World", srcHash: hashB } };
    const { merged, outdated } = mergeCatalog(extracted, current);
    expect(merged.a).toEqual({ defaultMessage: "Hello", translated: false, srcHash: hashA });
    expect(outdated).toEqual(["a"]);
    // b unchanged
    expect(merged.b).toEqual({ defaultMessage: "World", srcHash: hashB });
  });

  it("treats a legacy entry with no srcHash as outdated (bootstrap: forces re-hash)", () => {
    const current = { a: { defaultMessage: "Hello" }, b: { defaultMessage: "World" } };
    const { merged, outdated } = mergeCatalog(extracted, current);
    expect(merged.a).toEqual({ defaultMessage: "Hello", translated: false, srcHash: hashA });
    expect(outdated).toEqual(["a", "b"]);
  });

  it("drops keys absent from the source and reports them", () => {
    const current = {
      a: { defaultMessage: "Hello", srcHash: hashA },
      b: { defaultMessage: "World", srcHash: hashB },
      gone: { defaultMessage: "Stale", srcHash: "x" },
    };
    const { merged, stale } = mergeCatalog(extracted, current);
    expect(merged).not.toHaveProperty("gone");
    expect(stale).toEqual(["gone"]);
  });

  it("orders merged keys by source order", () => {
    const current = { b: { defaultMessage: "World", srcHash: hashB }, a: { defaultMessage: "Hello", srcHash: hashA } };
    const { merged } = mergeCatalog(extracted, current);
    expect(Object.keys(merged)).toEqual(["a", "b"]);
  });
});

describe("mergeCatalog — source locale", () => {
  it("seeds new keys without translated flag or srcHash", () => {
    const current = { a: { defaultMessage: "Ahoj" } };
    const { merged, added } = mergeCatalog(extracted, current, { isSource: true });
    expect(added).toEqual(["b"]);
    expect(merged.b).toEqual({ defaultMessage: "Svět" });
    expect(merged.b).not.toHaveProperty("translated");
    expect(merged.b).not.toHaveProperty("srcHash");
  });

  it("never flags the source outdated even when its stored text differs", () => {
    const current = { a: { defaultMessage: "old" }, b: { defaultMessage: "old" } };
    const { merged, outdated } = mergeCatalog(extracted, current, { isSource: true });
    expect(outdated).toEqual([]);
    // source is rewritten from the extracted default, no flags
    expect(merged.a).toEqual({ defaultMessage: "Ahoj" });
  });
});

describe("hashMessage", () => {
  it("is stable and differs by content", () => {
    expect(hashMessage("Ahoj")).toBe(hashMessage("Ahoj"));
    expect(hashMessage("Ahoj")).not.toBe(hashMessage("Svět"));
  });
});
