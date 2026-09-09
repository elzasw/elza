/**
 * Session-local set of import batches the current user has initiated from a wrapping call
 * (Import ze souboru in a fund/AE ribbon). ImportBatchToaster consumes it to show the final
 * outcome toast on any page, not just /admin/import.
 */
const pending = new Set<number>();

export function trackImportBatch(id: number): void {
    pending.add(id);
}

export function consumeImportBatch(id: number): boolean {
    return pending.delete(id);
}

export function isImportBatchPending(id: number): boolean {
    return pending.has(id);
}
