import {ExtAsyncQueueState} from "./ExtAsyncQueueState";

export interface ExtSyncsQueueItemVO {
    /**
     * Identifikátor.
     * @type {number}
     * @memberof ExtSyncsQueueItemVO
     */
    id: number;

    /**
     * Oblast entit.
     * @type {number}
     * @memberof ExtSyncsQueueItemVO
     */
    scopeId: number;

    /**
     * Stav.
     * @type {ExtAsyncQueueState}
     * @memberof ExtSyncsQueueItemVO
     */
    state: ExtAsyncQueueState;

    /**
     * Popis stavu.
     * @type {string}
     * @memberof ExtSyncsQueueItemVO
     */
    stateMessage?: string;

    /**
     * Indetifikátor přístupového bodu.
     * @type {number}
     * @memberof ExtSyncsQueueItemVO
     */
    accessPointId: number;

    /**
     * Název přístupového bodu.
     * @type {string}
     * @memberof ExtSyncsQueueItemVO
     */
    accessPointName: string;

    /**
     * Datum požadavku.
     * @type {string}
     * @memberof ExtSyncsQueueItemVO
     */
    date: string;

}
