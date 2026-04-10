import { DataType, NodeItem } from "elza-api";
import { useEffect, useState } from "react";

export function useLocalStorage<T>(key: string) {
  function save(data: T) {
    localStorage.setItem(key, JSON.stringify(data));
  }

  function load(): T {
    const data = localStorage.getItem(key);
    return JSON.parse(data);
  }

  function reset() {
    localStorage.removeItem(key);
  }

  return [save, load, reset] as [(data: T) => void, () => T, () => void];
}

export function createLocalStorageItemKey(item: NodeItem) {
  return `descItem-${item.nodeId}-${item.itemTypeId}-${item.itemObjectId || "new"}`;
}

export function useValueManager<T>(initialValue: T, item: NodeItem) {
  const [save, load, reset] = useLocalStorage<T>(
    createLocalStorageItemKey(item),
  );

  const storedValue = load();
  // const initialValue = data?.integerValue;

  const [value, setValue] = useState(storedValue || initialValue || null);
  const [isDirty, setIsDirty] = useState(initialValue != value);
  const [conflictValue, setConflictValue] = useState<T>(
    storedValue ? initialValue : null,
  );
  // Assign conflict value when initialValue changes
  // and the current value is dirty
  useEffect(() => {
    if (!isDirty) {
      setValue(initialValue);
    } else if (isDirty) {
      setConflictValue(initialValue);
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
