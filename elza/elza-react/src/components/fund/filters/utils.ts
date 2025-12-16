import { useEffect } from "react";

export function useInitialFocus<T extends HTMLElement>(ref: React.RefObject<T>) {
  useEffect(() => {
    if (ref.current) {
      ref.current.focus();
    }
  }, [ref]);
}
