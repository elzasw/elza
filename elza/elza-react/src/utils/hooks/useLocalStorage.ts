export function useLocalStorage<T>(key: string) {
  function save(data: T) {
    localStorage.setItem(key, JSON.stringify(data));
  }

  function load(): T | null {
    const data = localStorage.getItem(key);
    return data !== null ? JSON.parse(data) : null;
  }

  function reset() {
    localStorage.removeItem(key);
  }

  return [save, load, reset] as [(data: T) => void, () => T | null, () => void];
}
