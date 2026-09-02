import { act, renderHook } from "@testing-library/react";
import { DataType, NodeItem } from "elza-api";
import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import {
  clearLocalStorageItemValues,
  clearStaleNewItemValues,
  createLocalStorageItemKey,
  isEmptyItemValue,
  moveLocalStorageItemValues,
  useValueManager,
} from "./utils";

// jsdom in this setup does not provide localStorage - supply an in-memory replacement.
function installStorageMock() {
  if (typeof (globalThis as Record<string, unknown>).localStorage === "undefined") {
    const store = new Map<string, string>();
    const mock: Storage = {
      getItem: key => (store.has(key) ? store.get(key)! : null),
      setItem: (key, value) => void store.set(key, String(value)),
      removeItem: key => void store.delete(key),
      clear: () => store.clear(),
      key: index => Array.from(store.keys())[index] ?? null,
      get length() {
        return store.size;
      },
    };
    Object.defineProperty(globalThis, "localStorage", { value: mock, configurable: true });
  }
}

beforeAll(installStorageMock);

afterEach(() => localStorage.clear());

function item(data: Record<string, unknown> | undefined, rest: Partial<NodeItem> = {}): NodeItem {
  return { itemTypeId: 1, nodeId: 1, nodeVersion: 1, position: 1, data, ...rest } as NodeItem;
}

describe("isEmptyItemValue", () => {
  it("treats a missing data payload as empty", () => {
    expect(isEmptyItemValue(item(undefined))).toBe(true);
  });

  it("never reports an undefined-flagged item as empty", () => {
    expect(isEmptyItemValue(item(undefined, { undefined: true }))).toBe(false);
  });

  it.each([
    ["blank string", { dataType: DataType.String, stringValue: "   " }],
    ["missing string", { dataType: DataType.String }],
    ["blank text", { dataType: DataType.Text, textValue: "" }],
    ["missing int", { dataType: DataType.Int }],
    ["missing decimal", { dataType: DataType.Decimal }],
    ["blank unitid", { dataType: DataType.Unitid, unitId: "" }],
    ["missing date", { dataType: DataType.Date }],
    ["missing unitdate", { dataType: DataType.Unitdate }],
    ["blank coordinates", { dataType: DataType.Coordinates, value: "" }],
    ["missing uriRef", { dataType: DataType.UriRef }],
    ["uriRef without a uri", { dataType: DataType.UriRef, value: "  " }],
    ["uriRef keeping only a description", { dataType: DataType.UriRef, value: "", description: "note" }],
    ["uriRef keeping only a template", { dataType: DataType.UriRef, value: "", refTemplateId: 7 }],
  ])("reports %s as empty", (_label, data) => {
    expect(isEmptyItemValue(item(data))).toBe(true);
  });

  it.each([
    ["integer zero", { dataType: DataType.Int, integerValue: 0 }],
    ["decimal zero", { dataType: DataType.Decimal, value: 0 }],
    ["string zero", { dataType: DataType.String, stringValue: "0" }],
    ["filled text", { dataType: DataType.Text, textValue: "abc" }],
    ["filled uriRef", { dataType: DataType.UriRef, value: "http://example.org" }],
  ])("does not report %s as empty", (_label, data) => {
    expect(isEmptyItemValue(item(data))).toBe(false);
  });

  it("ignores data types it does not cover", () => {
    expect(isEmptyItemValue(item({ dataType: DataType.Bit }))).toBe(false);
    expect(isEmptyItemValue(item({ dataType: DataType.Structured }))).toBe(false);
  });
});

describe("createLocalStorageItemKey", () => {
  const stored = item(undefined, { nodeId: 5, itemTypeId: 9, itemObjectId: 12 });

  it("builds the key from the item identifiers", () => {
    expect(createLocalStorageItemKey(stored)).toBe("descItem-5-9-12");
  });

  it("identifies an item without an object id by its slot in the form", () => {
    const unsaved = item(undefined, { nodeId: 5, itemTypeId: 9, itemSpecId: 4, position: 2 });

    expect(createLocalStorageItemKey(unsaved)).toBe("descItem-5-9-new-4-2");
  });

  it("keeps two unsaved items of one type apart", () => {
    const first = item(undefined, { nodeId: 5, itemTypeId: 9, position: 1 });
    const second = item(undefined, { nodeId: 5, itemTypeId: 9, position: 2 });

    expect(createLocalStorageItemKey(first)).not.toBe(createLocalStorageItemKey(second));
  });

  it("keeps two specifications of one type apart", () => {
    const first = item(undefined, { nodeId: 5, itemTypeId: 9, itemSpecId: 1, position: 1 });
    const second = item(undefined, { nodeId: 5, itemTypeId: 9, itemSpecId: 2, position: 1 });

    expect(createLocalStorageItemKey(first)).not.toBe(createLocalStorageItemKey(second));
  });

  it("separates fields of one item so they cannot share an entry", () => {
    expect(createLocalStorageItemKey(stored, "description")).toBe("descItem-5-9-12-description");
    expect(createLocalStorageItemKey(stored, "description")).not.toBe(createLocalStorageItemKey(stored));
  });
});

describe("clearLocalStorageItemValues", () => {
  const removed = item(undefined, { nodeId: 1, itemTypeId: 2, itemObjectId: 3 });

  it("removes the item's own entry", () => {
    localStorage.setItem(createLocalStorageItemKey(removed), JSON.stringify("pending"));

    clearLocalStorageItemValues(removed);

    expect(localStorage.getItem(createLocalStorageItemKey(removed))).toBeNull();
  });

  it("removes the per-field entries of the same item", () => {
    localStorage.setItem(createLocalStorageItemKey(removed), JSON.stringify("uri"));
    localStorage.setItem(createLocalStorageItemKey(removed, "description"), JSON.stringify("note"));

    clearLocalStorageItemValues(removed);

    expect(localStorage.getItem(createLocalStorageItemKey(removed, "description"))).toBeNull();
    expect(localStorage.length).toBe(0);
  });

  it("keeps entries of an item whose object id merely shares a prefix", () => {
    const similar = item(undefined, { nodeId: 1, itemTypeId: 2, itemObjectId: 30 });
    localStorage.setItem(createLocalStorageItemKey(similar), JSON.stringify("other"));

    clearLocalStorageItemValues(removed);

    expect(localStorage.getItem(createLocalStorageItemKey(similar))).toBe(JSON.stringify("other"));
  });

  it("keeps entries of other items on the same node", () => {
    const sibling = item(undefined, { nodeId: 1, itemTypeId: 9, itemObjectId: 4 });
    localStorage.setItem(createLocalStorageItemKey(sibling), JSON.stringify("sibling"));

    clearLocalStorageItemValues(removed);

    expect(localStorage.getItem(createLocalStorageItemKey(sibling))).toBe(JSON.stringify("sibling"));
  });

  it("does nothing when the item has no pending value", () => {
    expect(() => clearLocalStorageItemValues(removed)).not.toThrow();
  });

  it("removes the entry of an item that was never saved", () => {
    const unsaved = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
    localStorage.setItem(createLocalStorageItemKey(unsaved), JSON.stringify("typed"));

    clearLocalStorageItemValues(unsaved);

    expect(localStorage.getItem("descItem-1-2-new-0-1")).toBeNull();
  });

  it("does not touch the entry of an unsaved item when clearing a saved item of the same type", () => {
    const unsaved = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
    localStorage.setItem(createLocalStorageItemKey(unsaved), JSON.stringify("typed"));

    clearLocalStorageItemValues(removed);

    expect(localStorage.getItem("descItem-1-2-new-0-1")).toBe(JSON.stringify("typed"));
  });

  it("clears by the position it is given, not by the one the item was created on", () => {
    const submitted = item(undefined, { nodeId: 1, itemTypeId: 2, position: 3 });
    const created = item(undefined, { nodeId: 1, itemTypeId: 2, position: 7, itemObjectId: 11 });
    localStorage.setItem(createLocalStorageItemKey(submitted), JSON.stringify("typed"));

    clearLocalStorageItemValues(created);
    expect(localStorage.getItem(createLocalStorageItemKey(submitted))).toBe(JSON.stringify("typed"));

    clearLocalStorageItemValues(submitted);
    expect(localStorage.getItem(createLocalStorageItemKey(submitted))).toBeNull();
  });
});

describe("moveLocalStorageItemValues", () => {
  const before = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
  const after = item(undefined, { nodeId: 1, itemTypeId: 2, position: 4 });

  it("carries the pending value over to the new key", () => {
    localStorage.setItem(createLocalStorageItemKey(before), JSON.stringify("typed"));

    moveLocalStorageItemValues([{ fromItem: before, toItem: after }]);

    expect(localStorage.getItem(createLocalStorageItemKey(before))).toBeNull();
    expect(localStorage.getItem(createLocalStorageItemKey(after))).toBe(JSON.stringify("typed"));
  });

  it("carries the per-field entries over as well", () => {
    localStorage.setItem(createLocalStorageItemKey(before, "description"), JSON.stringify("note"));

    moveLocalStorageItemValues([{ fromItem: before, toItem: after }]);

    expect(localStorage.getItem(createLocalStorageItemKey(after, "description"))).toBe(JSON.stringify("note"));
  });

  it("leaves an item that keeps its key untouched", () => {
    localStorage.setItem(createLocalStorageItemKey(before), JSON.stringify("typed"));

    moveLocalStorageItemValues([{ fromItem: before, toItem: { ...before } }]);

    expect(localStorage.getItem(createLocalStorageItemKey(before))).toBe(JSON.stringify("typed"));
  });

  it("keeps the values apart when a whole group shifts by one position", () => {
    const first = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
    const second = item(undefined, { nodeId: 1, itemTypeId: 2, position: 2 });
    localStorage.setItem(createLocalStorageItemKey(first), JSON.stringify("first"));
    localStorage.setItem(createLocalStorageItemKey(second), JSON.stringify("second"));

    moveLocalStorageItemValues([
      { fromItem: first, toItem: { ...first, position: 2 } },
      { fromItem: second, toItem: { ...second, position: 3 } },
    ]);

    expect(localStorage.getItem("descItem-1-2-new-0-2")).toBe(JSON.stringify("first"));
    expect(localStorage.getItem("descItem-1-2-new-0-3")).toBe(JSON.stringify("second"));
    expect(localStorage.getItem("descItem-1-2-new-0-1")).toBeNull();
  });
});

describe("clearStaleNewItemValues", () => {
  const live = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
  const gone = item(undefined, { nodeId: 1, itemTypeId: 2, position: 2 });

  it("keeps the entries of items the form still shows", () => {
    localStorage.setItem(createLocalStorageItemKey(live), JSON.stringify("typed"));
    localStorage.setItem(createLocalStorageItemKey(live, "description"), JSON.stringify("note"));

    clearStaleNewItemValues(1, [live]);

    expect(localStorage.getItem(createLocalStorageItemKey(live))).toBe(JSON.stringify("typed"));
    expect(localStorage.getItem(createLocalStorageItemKey(live, "description"))).toBe(JSON.stringify("note"));
  });

  it("drops the entry of an unsaved item the form no longer shows", () => {
    localStorage.setItem(createLocalStorageItemKey(gone), JSON.stringify("lost response"));

    clearStaleNewItemValues(1, [live]);

    expect(localStorage.getItem(createLocalStorageItemKey(gone))).toBeNull();
  });

  it("leaves saved items and other nodes alone", () => {
    const saved = item(undefined, { nodeId: 1, itemTypeId: 2, itemObjectId: 3 });
    const otherNode = item(undefined, { nodeId: 8, itemTypeId: 2, position: 2 });
    localStorage.setItem(createLocalStorageItemKey(saved), JSON.stringify("saved"));
    localStorage.setItem(createLocalStorageItemKey(otherNode), JSON.stringify("other node"));

    clearStaleNewItemValues(1, [live]);

    expect(localStorage.getItem(createLocalStorageItemKey(saved))).toBe(JSON.stringify("saved"));
    expect(localStorage.getItem(createLocalStorageItemKey(otherNode))).toBe(JSON.stringify("other node"));
  });

  it("drops entries left by the previous key format", () => {
    localStorage.setItem("descItem-1-2-new", JSON.stringify("legacy"));

    clearStaleNewItemValues(1, [live]);

    expect(localStorage.getItem("descItem-1-2-new")).toBeNull();
  });
});

describe("useValueManager", () => {
  const managed = item(undefined, { nodeId: 1, itemTypeId: 2, itemObjectId: 3 });
  const key = createLocalStorageItemKey(managed);

  function renderManager(initialValue: string, field?: string) {
    return renderHook(({ value }: { value: string }) => useValueManager(value, managed, field), {
      initialProps: { value: initialValue },
    });
  }

  it("starts clean on the server value when nothing is stored", () => {
    const { result } = renderManager("server");

    expect(result.current.value).toBe("server");
    expect(result.current.isDirty).toBe(false);
    expect(result.current.conflictValue).toBeNull();
  });

  it("restores a pending value from storage and reports the server value as a conflict", () => {
    localStorage.setItem(key, JSON.stringify("pending"));

    const { result } = renderManager("server");

    expect(result.current.value).toBe("pending");
    expect(result.current.conflictValue).toBe("server");
  });

  it("drops a stored value the server has caught up with", () => {
    localStorage.setItem(key, JSON.stringify("server"));

    const { result } = renderManager("server");

    expect(result.current.value).toBe("server");
    expect(result.current.conflictValue).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
  });

  it("mirrors an edited value into storage and marks it dirty", () => {
    const { result } = renderManager("server");

    act(() => result.current.setValue("edited"));

    expect(result.current.isDirty).toBe(true);
    expect(localStorage.getItem(key)).toBe(JSON.stringify("edited"));
  });

  it("clears storage once the value returns to the server value", () => {
    const { result } = renderManager("server");

    act(() => result.current.setValue("edited"));
    act(() => result.current.setValue("server"));

    expect(result.current.isDirty).toBe(false);
    expect(localStorage.getItem(key)).toBeNull();
  });

  it("keeps the edit and records a conflict when the server value changes underneath", () => {
    const { result, rerender } = renderManager("server");

    act(() => result.current.setValue("edited"));
    rerender({ value: "remote" });

    expect(result.current.value).toBe("edited");
    expect(result.current.conflictValue).toBe("remote");
  });

  it("follows the server value when there is no pending edit", () => {
    const { result, rerender } = renderManager("server");

    rerender({ value: "remote" });

    expect(result.current.value).toBe("remote");
    expect(result.current.conflictValue).toBeNull();
  });

  it("takes over the server value when the conflict is reset", () => {
    const { result, rerender } = renderManager("server");

    act(() => result.current.setValue("edited"));
    rerender({ value: "remote" });
    act(() => result.current.resetConflict());

    expect(result.current.value).toBe("remote");
    expect(result.current.conflictValue).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
  });

  it("clears the pending state once a change is saved", () => {
    const { result } = renderManager("server");

    act(() => result.current.setValue("edited"));
    act(() => result.current.finishChange());

    expect(result.current.isDirty).toBe(false);
    expect(localStorage.getItem(key)).toBeNull();
  });

  it("mirrors the pending value of an item that has no object id", () => {
    const unsaved = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
    const { result } = renderHook(() => useValueManager<string>(undefined, unsaved));

    act(() => result.current.setValue("typed"));

    expect(result.current.isDirty).toBe(true);
    expect(localStorage.getItem(createLocalStorageItemKey(unsaved))).toBe(JSON.stringify("typed"));
  });

  it("restores the pending value of an item that has no object id", () => {
    const unsaved = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
    localStorage.setItem(createLocalStorageItemKey(unsaved), JSON.stringify("typed"));

    const { result } = renderHook(() => useValueManager<string>(undefined, unsaved));

    expect(result.current.value).toBe("typed");
    expect(result.current.isDirty).toBe(true);
  });

  it("does not hand the pending value to the next empty item of the same type", () => {
    const filled = item(undefined, { nodeId: 1, itemTypeId: 2, position: 1 });
    const added = item(undefined, { nodeId: 1, itemTypeId: 2, position: 2 });
    const { result } = renderHook(() => useValueManager<string>(undefined, filled));

    act(() => result.current.setValue("typed"));
    const { result: addedResult } = renderHook(() => useValueManager<string>(undefined, added));

    expect(addedResult.current.value).toBeNull();
    expect(addedResult.current.isDirty).toBe(false);
  });

  it("does not cache the value of an item edited outside a node", () => {
    const structureItem = item(undefined, { nodeId: undefined, itemTypeId: 2, position: 1 });
    localStorage.setItem(createLocalStorageItemKey(structureItem), JSON.stringify("other structure"));

    const { result } = renderHook(() => useValueManager<string>(undefined, structureItem));
    expect(result.current.value).toBeNull();

    act(() => result.current.setValue("typed"));

    expect(result.current.isDirty).toBe(true);
    expect(localStorage.getItem(createLocalStorageItemKey(structureItem))).toBe(
      JSON.stringify("other structure"),
    );
  });

  it("reads storage only while mounting, not on every render", () => {
    const { rerender } = renderManager("server");
    const getItem = vi.spyOn(localStorage, "getItem");

    rerender({ value: "server" });
    rerender({ value: "server" });

    expect(getItem).not.toHaveBeenCalled();
    getItem.mockRestore();
  });

  it("keeps the fields of one item apart", () => {
    localStorage.setItem(createLocalStorageItemKey(managed, "description"), JSON.stringify("note"));

    const { result } = renderManager("server");

    expect(result.current.value).toBe("server");
    expect(result.current.conflictValue).toBeNull();
  });
});
