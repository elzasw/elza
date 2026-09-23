import {AsyncType} from 'elza-api';

/**
 * Order the queues are listed in; the server returns them in the arbitrary
 * order of its executor map.
 */
export const QUEUE_TYPES: readonly AsyncType[] = ['NODE', 'BULK', 'OUTPUT', 'AP', 'EXPORT', 'AIP', 'BATCH_IMPORT'];

/**
 * Compares queues by {@link QUEUE_TYPES}, so that the list keeps one order
 * regardless of what the server sends. A type the client does not know yet
 * sorts last instead of disappearing.
 */
export function compareQueueType(a: AsyncType, b: AsyncType): number {
    const rank = (type: AsyncType) => {
        const index = QUEUE_TYPES.indexOf(type);
        return index === -1 ? QUEUE_TYPES.length : index;
    };
    return rank(a) - rank(b);
}
