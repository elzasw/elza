/**
 * Výsledek hledání - zvýraznění obsažených požadovaných částí
 * @export
 * @interface ResultLookupVO
 */
import {HighlightVO} from "./HighlightVO";

export interface ResultLookupVO {
    /**
     * Celá textová hodnota
     * @type {string}
     * @memberof ResultLookupVO
     */
    value: string;
    /**
     *
     * @type {PartType}
     * @memberof ResultLookupVO
     */
    partTypeCode: string;
    /**
     * Seznam pozic pro zvýraznění
     * @type {Array<HighlightVO>}
     * @memberof ResultLookupVO
     */
    highlights: Array<HighlightVO>;
}


