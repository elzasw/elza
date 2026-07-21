import { DataType, NodeItem } from "elza-api";
import { useEffect, useState } from "react";
import { useLocalStorage } from "utils/hooks/useLocalStorage";

export function createLocalStorageItemKey(item: NodeItem) {
  return `descItem-${item.nodeId}-${item.itemTypeId}-${item.itemObjectId || "new"}`;
}

export function useValueManager<T extends string | number>(initialValue: T, item: NodeItem) {
  const [save, load, reset] = useLocalStorage<T>(
    createLocalStorageItemKey(item),
  );

  const storedValue = load();
  const hasStaleStorage = storedValue != null && storedValue == initialValue;
  if (hasStaleStorage) { reset(); }

  const effectiveStoredValue = hasStaleStorage ? null : storedValue;

  const [value, setValue] = useState(effectiveStoredValue ?? initialValue ?? null);
  const [isDirty, setIsDirty] = useState(initialValue != value);
  const [conflictValue, setConflictValue] = useState<T>(
    effectiveStoredValue ? initialValue : null,
  );
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

  // Manage dirty state with local storage
  // depending on value change
  useEffect(() => {
    const _isDirty = value != initialValue;
    setIsDirty(_isDirty);

    if (_isDirty) {
      if (value != storedValue) {
        save(value);
      }
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
