/**
 * Filter pro vyhledávání synchronizace k CAM.
 *
 * @interface SyncsFilterVO
 */
import {ExtAsyncQueueState} from "./ExtAsyncQueueState";

export interface SyncsFilterVO {

    /**
     * Stavy položek fronty.
     * @type {Array<ExtAsyncQueueState>}
     * @memberof SyncsFilterVO
     */
    states?: Array<ExtAsyncQueueState>;

    /**
     * Kódy oblastí entit.
     * @type {Array<string>}
     * @memberof SyncsFilterVO
     */
    scopes?: Array<string>;
}


