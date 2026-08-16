import {useCallback, useRef, useState} from 'react';

/**
 * Sleduje šířku prvku pomocí ResizeObserver.
 * Vrací callback ref, který je potřeba připojit na měřený prvek, a jeho aktuální
 * šířku v px (null dokud není prvek změřen).
 *
 * Callback ref se použije místo objektového refu záměrně – prvek se může připojit
 * až po několika renderech (např. po dokončení načítání), takže observer je nutné
 * (od)pojit přesně ve chvíli, kdy se DOM prvek objeví nebo zmizí.
 */
export function useContainerWidth<T extends HTMLElement>() {
    const [width, setWidth] = useState<number | null>(null);
    const observerRef = useRef<ResizeObserver | null>(null);

    const ref = useCallback((element: T | null) => {
        observerRef.current?.disconnect();

        if (!element) {
            observerRef.current = null;
            return;
        }

        const observer = new ResizeObserver(entries => {
            const entry = entries[0];
            if (entry) {
                setWidth(entry.contentRect.width);
            }
        });
        observer.observe(element);
        observerRef.current = observer;
    }, []);

    return [ref, width] as const;
}
