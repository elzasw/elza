import { Api } from 'api';
import { UploadLimits } from 'elza-api';
import { useEffect, useState } from 'react';

let limitsPromise: Promise<UploadLimits> | null = null;

function loadUploadLimits(): Promise<UploadLimits> {
    if (!limitsPromise) {
        limitsPromise = Api.importBatches.importBatchUploadLimits().then(({ data }) => data);
        // a failed load is retried by the next component that asks
        limitsPromise.catch(() => {
            limitsPromise = null;
        });
    }
    return limitsPromise;
}

/**
 * Upload size limits of the server, loaded once per page. Returns null until they arrive;
 * a limit of -1 means the server applies none.
 */
export function useUploadLimits(): UploadLimits | null {
    const [limits, setLimits] = useState<UploadLimits | null>(null);

    useEffect(() => {
        let cancelled = false;
        loadUploadLimits()
            .then(data => {
                if (!cancelled) setLimits(data);
            })
            .catch(() => {
                // the global API error handler has already reported it
            });
        return () => {
            cancelled = true;
        };
    }, []);

    return limits;
}

/** True when the file exceeds the given limit; a negative limit means no limit. */
export function exceedsUploadLimit(file: File, limit: number): boolean {
    return limit >= 0 && file.size > limit;
}
