/**
 * Filter pro vztah
 * @export
 * @interface RelationFilterVO
 */
export interface RelationFilterVO {
    /**
     * Identifikátor typu atributu pro vztah
     * @type {number}
     * @memberof RelationFilterVO
     */
    relTypeId?: number;
    /**
     * Identifikátor specifikace atributu pro vztah
     * @type {number}
     * @memberof RelationFilterVO
     */
    relSpecId?: number;
    /**
     * Globální identifikátor archivní entity
     * @type {number}
     * @memberof RelationFilterVO
     */
    code?: number;
}


