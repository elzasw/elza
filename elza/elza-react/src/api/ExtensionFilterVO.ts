/**
 * Rozšířený filter
 * @export
 * @interface ExtensionFilterVO
 */
export interface ExtensionFilterVO {
    /**
     *
     * @type {PartType}
     * @memberof ExtensionFilterVO
     */
    partTypeCode?: string;
    /**
     * Identifikátor typu atributu
     * @type {number}
     * @memberof ExtensionFilterVO
     */
    itemTypeId?: number;
    /**
     * Identifikátor specifikace atributu
     * @type {number}
     * @memberof ExtensionFilterVO
     */
    itemSpecId?: number;
    /**
     * Hodnota atributu
     * @type {object}
     * @memberof ExtensionFilterVO
     */
    value?: object;
}


