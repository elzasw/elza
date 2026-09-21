/**
 * Types of the asynchronous request queues, mirroring the server `AsyncTypeEnum`.
 *
 * The order is the order the queues are listed in; the server returns them in
 * the arbitrary order of its executor map.
 */
export const QUEUE_TYPES = ['NODE', 'BULK', 'OUTPUT', 'AP', 'EXPORT', 'AIP', 'BATCH_IMPORT'] as const;

export type QueueType = (typeof QUEUE_TYPES)[number];

/** One worker thread of a queue - server `AsyncWorkerVO`. */
export interface QueueWorker {
    fundVersionId: number | null;
    requestId: number | null;
    /** Server `LocalDateTime`, i.e. without a zone - `2026-09-21T08:15:30`. */
    beginTime: string | null;
    runningTime: number | null;
    currentId: number | null;
}

/** State of a single queue - server `ArrAsyncRequestVO`. */
export interface QueueInfo {
    type: QueueType;
    /** Share of the capacity in use, 0..1. */
    load: number;
    requestPerHour: number;
    waitingRequests: number;
    runningThreadCount: number;
    totalThreadCount: number;
    currentThreads: QueueWorker[];
}

/** Requests waiting in a queue for one fund - server `FundStatisticsVO`. */
export interface QueueFundStats {
    fund: {id: number; name: string} | null;
    fundVersionId: number | null;
    requestCount: number;
}

/**
 * Compares queues by {@link QUEUE_TYPES}, so that the list keeps one order
 * regardless of what the server sends. A type the client does not know yet
 * sorts last instead of disappearing.
 */
export function compareQueueType(a: QueueType, b: QueueType): number {
    const rank = (type: QueueType) => {
        const index = QUEUE_TYPES.indexOf(type);
        return index === -1 ? QUEUE_TYPES.length : index;
    };
    return rank(a) - rank(b);
}
