/**
 * výsledek hledání
 * @export
 * @interface ArchiveEntityResultListVO
 */
import {ArchiveEntityVO} from "./ArchiveEntityVO";

export interface ArchiveEntityResultListVO {
    /**
     * celkový počet archivních entit
     * @type {number}
     * @memberof ArchiveEntityResultListVO
     */
    total: number;
    /**
     * výsledky
     * @type {Array<ArchiveEntityVO>}
     * @memberof ArchiveEntityResultListVO
     */
    data: Array<ArchiveEntityVO>;
}
