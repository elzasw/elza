import { useState } from 'react';

export function useLocalStorageState<T>(key: string, initialValue: T): [T, (value: T) => void] {
    const [storedValue, setStoredValue] = useState<T>(() => {
        const item = localStorage.getItem(key);
        return item !== null ? JSON.parse(item) : initialValue;
    });

    const setValue = (value: T) => {
        localStorage.setItem(key, JSON.stringify(value));
        setStoredValue(value);
    };

    return [storedValue, setValue];
}
