/**
 * výsledek hledání
 * @export
 * @interface ExtSyncsQueueResultListVO
 */
import {ExtSyncsQueueItemVO} from "./ExtSyncsQueueItemVO";

export interface ExtSyncsQueueResultListVO {
    /**
     * celkový počet
     * @type {number}
     * @memberof ExtSyncsQueueResultListVO
     */
    total: number;
    /**
     * výsledky
     * @type {Array<ExtSyncsQueueItemVO>}
     * @memberof ExtSyncsQueueResultListVO
     */
    data: Array<ExtSyncsQueueItemVO>;
}
