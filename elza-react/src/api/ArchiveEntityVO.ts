import {ResultLookupVO} from "./ResultLookupVO";

export interface ArchiveEntityVO {
    /**
     * Identifikátor.
     * @type {number}
     * @memberof ArchiveEntityVO
     */
    id: number;
    /**
     * Název archivní entity (preferované jméno)
     * @type {string}
     * @memberof ArchiveEntityVO
     */
    name: string;
    /**
     * Identifikátor typu archivní entity
     * @type {number}
     * @memberof ArchiveEntityVO
     */
    aeTypeId: number;
    /**
     * Popis
     * @type {string}
     * @memberof ArchiveEntityVO
     */
    description?: string;
    /**
     *
     * @type {ResultLookupVO}
     * @memberof ArchiveEntityVO
     */
    resultLookups: ResultLookupVO;
}
