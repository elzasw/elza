import {useCallback, useEffect, useRef, useState} from 'react';

export interface PolledData<T> {
    /** Last successfully loaded value, `undefined` until the first one arrives. */
    data: T | undefined;
    /** True when the most recent attempt failed; the previous `data` is kept. */
    failed: boolean;
    /** Loads immediately and restarts the interval. */
    refresh: () => void;
}

/**
 * Keeps a value refreshed from the server.
 *
 * The next load is scheduled only once the previous one settles, so a server
 * slower than the interval does not accumulate requests. A failed load does not
 * end the cycle - the next attempt follows in the usual interval - and it keeps
 * the last value on screen instead of blanking the page.
 *
 * Nothing is loaded while the tab is hidden; the cycle catches up as soon as it
 * becomes visible again.
 *
 * @param load loader; it is read through a ref, so an inline arrow function
 *             does not restart the cycle on every render
 * @param intervalMs delay between loads
 */
export function usePolledData<T>(load: () => Promise<T>, intervalMs: number): PolledData<T> {
    const [data, setData] = useState<T>();
    const [failed, setFailed] = useState(false);
    const [reloadToken, setReloadToken] = useState(0);

    const loadRef = useRef(load);
    useEffect(() => {
        loadRef.current = load;
    });

    useEffect(() => {
        let cancelled = false;
        let timer: ReturnType<typeof setTimeout> | undefined;

        const schedule = () => {
            timer = setTimeout(run, intervalMs);
        };

        const run = () => {
            if (document.hidden) {
                schedule();
                return;
            }
            loadRef.current().then(
                result => {
                    if (cancelled) {
                        return;
                    }
                    setData(result);
                    setFailed(false);
                    schedule();
                },
                () => {
                    if (cancelled) {
                        return;
                    }
                    setFailed(true);
                    schedule();
                },
            );
        };

        const handleVisibilityChange = () => {
            if (!document.hidden) {
                clearTimeout(timer);
                run();
            }
        };

        run();
        document.addEventListener('visibilitychange', handleVisibilityChange);

        return () => {
            cancelled = true;
            clearTimeout(timer);
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, [intervalMs, reloadToken]);

    const refresh = useCallback(() => setReloadToken(token => token + 1), []);

    return {data, failed, refresh};
}
