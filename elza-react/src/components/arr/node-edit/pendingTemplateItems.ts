/**
 * Stores pending template callbacks for newly created nodes.
 * Set after node creation in afterCreateCallback, consumed by useNodeFormData on first data load.
 */

type AddEmptyDescItemFn = (typeId: number, specId?: number) => void;
type PendingCallback = (addEmptyDescItem: AddEmptyDescItemFn) => void;

const pendingCallbacks = new Map<number, PendingCallback>();

/** Register a callback to add empty template items for a newly created node. */
export function setPendingTemplateCallback(nodeId: number, callback: PendingCallback) {
    pendingCallbacks.set(nodeId, callback);
}

/** Retrieve and clear the pending callback for a node. Clears entire map since it should be consumed immediately. */
export function consumePendingTemplateCallback(nodeId: number): PendingCallback | undefined {
    const callback = pendingCallbacks.get(nodeId);
    pendingCallbacks.clear();
    return callback;
}
