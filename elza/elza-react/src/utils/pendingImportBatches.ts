/**
 * Session-local set of import batches the current user has initiated from a wrapping call
 * (Import ze souboru in a fund/AE ribbon). ImportBatchToaster consumes it to show the final
 * outcome toast on any page, not just /admin/import.
 *
 * Tracking a batch also notifies the listeners, so the component that owns the batch messaging
 * can announce the enqueue as well - the callers are redux actions with no intl context.
 */
const pending = new Set<number>();

type Listener = (id: number) => void;

const listeners = new Set<Listener>();

export function trackImportBatch(id: number): void {
    pending.add(id);
    listeners.forEach(listener => listener(id));
}

export function consumeImportBatch(id: number): boolean {
    return pending.delete(id);
}

export function isImportBatchPending(id: number): boolean {
    return pending.has(id);
}

/**
 * Subscribes to batches being tracked. Returns the unsubscribe function.
 */
export function onImportBatchTracked(listener: Listener): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}
