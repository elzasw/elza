import {
  DataCoordinates,
  DataDate,
  DataDecimal,
  DataFormattedText,
  DataInteger,
  DataString,
  DataText,
  DataType,
  DataUnitdate,
  DataUnitid,
  DataUriRef,
  NodeItem,
} from "elza-api";
import { useEffect, useState } from "react";
import { useLocalStorage } from "utils/hooks/useLocalStorage";

/**
 * Storage key for an item's pending value. An item saved on the server is identified by its
 * object id; an item that has none is identified by the slot it occupies in the form, because
 * the specification and the position are the only identity such an item has. `field` separates
 * components that edit several fields of one item.
 */
export function createLocalStorageItemKey(item: NodeItem, field?: string) {
  const key = item.itemObjectId
    ? `descItem-${item.nodeId}-${item.itemTypeId}-${item.itemObjectId}`
    : `descItem-${item.nodeId}-${item.itemTypeId}-new-${item.itemSpecId ?? 0}-${item.position ?? 0}`;
  return field ? `${key}-${field}` : key;
}

function collectStorageKeys(matches: (storedKey: string) => boolean) {
  const keys: string[] = [];

  for (let index = 0; index < localStorage.length; index++) {
    const storedKey = localStorage.key(index);
    if (storedKey != null && matches(storedKey)) {
      keys.push(storedKey);
    }
  }

  return keys;
}

function belongsToItemKey(storedKey: string, itemKey: string) {
  return storedKey === itemKey || storedKey.startsWith(`${itemKey}-`);
}

/**
 * Discard an item's pending values, including the per-field entries of components that edit
 * several fields. Once the item is gone from the form nothing mounts its key again, so the
 * entry has to be dropped from the outside.
 *
 * For an item with no object id, pass it exactly as it was when the value was stored - the
 * position is part of its key and the server can return the created item on another position.
 */
export function clearLocalStorageItemValues(item: NodeItem) {
  const itemKey = createLocalStorageItemKey(item);

  collectStorageKeys((storedKey) => belongsToItemKey(storedKey, itemKey)).forEach((staleKey) =>
    localStorage.removeItem(staleKey),
  );
}

/**
 * Follow items that keep their pending values but change their key, i.e. unsaved items moved to
 * another position.
 *
 * All sources are read and emptied before the first target is written: items of one type move as
 * a group, so the key an item moves to is often still held by an item that has not moved yet.
 */
export function moveLocalStorageItemValues(
  moves: { fromItem: NodeItem; toItem: NodeItem }[],
) {
  const movedValues: { key: string; value: string }[] = [];

  moves.forEach(({ fromItem, toItem }) => {
    const fromKey = createLocalStorageItemKey(fromItem);
    const toKey = createLocalStorageItemKey(toItem);

    if (fromKey === toKey) {
      return;
    }

    collectStorageKeys((storedKey) => belongsToItemKey(storedKey, fromKey)).forEach((storedKey) => {
      const fieldSuffix = storedKey.slice(fromKey.length);
      const storedValue = localStorage.getItem(storedKey);

      localStorage.removeItem(storedKey);
      if (storedValue != null) {
        movedValues.push({ key: `${toKey}${fieldSuffix}`, value: storedValue });
      }
    });
  });

  movedValues.forEach(({ key, value }) => localStorage.setItem(key, value));
}

/**
 * Drop pending values of unsaved items the form no longer shows. Such an entry is left behind
 * when the item reaches the server but the response never arrives (the page is closed while the
 * create request is in flight), and nothing would clear it from the inside afterwards.
 */
export function clearStaleNewItemValues(nodeId: number, liveItems: NodeItem[]) {
  const newItemKeyPattern = new RegExp(`^descItem-${nodeId}-\\d+-new(-|$)`);
  const liveItemKeys = liveItems
    .filter((item) => !item.itemObjectId)
    .map((item) => createLocalStorageItemKey(item));

  collectStorageKeys(
    (storedKey) =>
      newItemKeyPattern.test(storedKey) &&
      !liveItemKeys.some((liveItemKey) => belongsToItemKey(storedKey, liveItemKey)),
  ).forEach((staleKey) => localStorage.removeItem(staleKey));
}

export function useValueManager<T extends string | number>(initialValue: T, item: NodeItem, field?: string) {
  const [saveStoredValue, loadStoredValue, resetStoredValue] = useLocalStorage<T>(
    createLocalStorageItemKey(item, field),
  );

  // A pending value is only kept for a form the user can come back to. Items edited outside a
  // node - the structure dialog builds them without one - have no key to return to, and the
  // dialog starts from the stored data on every open, so such an item leaves storage alone.
  const isCacheable = item.nodeId != undefined;
  const save = (pendingValue: T) => isCacheable && saveStoredValue(pendingValue);
  const load = () => (isCacheable ? loadStoredValue() : null);
  const reset = () => isCacheable && resetStoredValue();

  // Read once on mount: localStorage access is synchronous, and the stored value only seeds
  // the initial state.
  const [storedValue] = useState(load);
  const hasStaleStorage = storedValue != null && storedValue == initialValue;

  const effectiveStoredValue = hasStaleStorage ? null : storedValue;

  const [value, setValue] = useState(effectiveStoredValue ?? initialValue ?? null);
  const [isDirty, setIsDirty] = useState(initialValue != value);
  const [conflictValue, setConflictValue] = useState<T>(
    effectiveStoredValue ? initialValue : null,
  );

  useEffect(() => {
    if (hasStaleStorage) { reset(); }
  }, []);
  // Assign conflict value when initialValue changes and the current value is dirty.
  // If the server caught up to the local value, clear dirty state instead.
  useEffect(() => {
    if (value == initialValue) {
      finishChange();
    } else if (isDirty) {
      setConflictValue(initialValue);
    } else {
      setValue(initialValue);
    }
  }, [initialValue]);

  // Mirror the pending value into local storage; a value matching the server needs no entry.
  useEffect(() => {
    const hasPendingValue = value != initialValue;
    setIsDirty(hasPendingValue);

    if (hasPendingValue) {
      save(value);
    } else if (!conflictValue) {
      reset();
    }
  }, [value]);

  function resetConflict() {
    setConflictValue(null);
    setValue(initialValue);
    reset();
  }

  function finishChange() {
    setIsDirty(false);
    reset();
  }

  return {
    value,
    setValue,
    initialValue,
    conflictValue,
    isDirty,
    resetConflict,
    finishChange,
  };
}

/**
 * Whether the item carries no value. Zero and "0" are values, not emptiness.
 *
 * Only the data types edited through `useValueManager` are covered; the remaining ones
 * (reference/spec based) never reach the empty-value branch.
 */
export function isEmptyItemValue(item: NodeItem): boolean {
  if (item.undefined) {
    return false;
  }

  const data = item.data;
  if (!data) {
    return true;
  }

  const isBlank = (value: unknown) =>
    value == null || (typeof value === "string" && value.trim() === "");

  switch (data.dataType) {
    case DataType.String:
      return isBlank((data as DataString).stringValue);
    case DataType.Text:
      return isBlank((data as DataText).textValue);
    case DataType.FormattedText:
      return isBlank((data as DataFormattedText).value);
    case DataType.Int:
      return isBlank((data as DataInteger).integerValue);
    case DataType.Decimal:
      return isBlank((data as DataDecimal).value);
    case DataType.Unitid:
      return isBlank((data as DataUnitid).unitId);
    case DataType.Date:
      return isBlank((data as DataDate).value);
    case DataType.Unitdate:
      return isBlank((data as DataUnitdate).value);
    case DataType.Coordinates:
      return isBlank((data as DataCoordinates).value);
    case DataType.UriRef:
      // The URI is required; the description and template only qualify it, so they cannot keep
      // the item alive on their own.
      return isBlank((data as DataUriRef).value);
    default:
      return false;
  }
}

export function createEmptyDescItem(
  itemTypeId: number,
  nodeId: number,
  nodeVersion: number,
  position: number = 1,
  dataTypeCode: DataType,
) {
  return {
    itemTypeId,
    nodeId,
    nodeVersion,
    position,
    data: {
      dataType: dataTypeCode,
    },
  };
}

export function findInSources(
  value: string,
  sources: string[],
  options: { ignoreCase: boolean; ignoreDiacritic: boolean } = {
    ignoreCase: true,
    ignoreDiacritic: true,
  },
) {
  const normalizedQuery = options.ignoreCase
    ? (value || "").toLowerCase()
    : value || "";

  const normalizedSources = [];
  sources.forEach((source) => {
    normalizedSources.push(options.ignoreCase ? source.toLowerCase() : source);
    if (options.ignoreDiacritic) {
      const withoutDiacritic = source
        .normalize("NFD")
        .replace(/\p{Diacritic}/gu, "")
        .toLowerCase();
      normalizedSources.push(
        options.ignoreCase ? withoutDiacritic.toLowerCase() : withoutDiacritic,
      );
    }
  });
  return (
    normalizedSources.find(
      (normalizedSource) => normalizedSource.indexOf(normalizedQuery) >= 0,
    ) != undefined
  );
}
