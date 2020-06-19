/**
 * Filter pro vyhledávání v jádru CAM
 * @export
 * @interface SearchFilterVO
 */
import {AeState} from "./AeState";
import {UserVO} from "./UserVO";
import {Area} from "./Area";
import {RelationFilterVO} from "./RelationFilterVO";
import {ExtensionFilterVO} from "./ExtensionFilterVO";

export interface SearchFilterVO {
    /**
     * Vyhledávané fráze
     * @type {string}
     * @memberof SearchFilterVO
     */
    search?: string;
    /**
     *
     * @type {Area}
     * @memberof SearchFilterVO
     */
    area?: Area;
    /**
     * Upřesnění, zda se vyhledává v celém označení nebo jen v hlavní části; povinné pokud {@link SearchFilterVO#area} je {@link Area#PREFERNAMES} nebo {@link Area#ALLNAMES}
     * @type {boolean}
     * @memberof SearchFilterVO
     */
    onlyMainPart?: boolean;
    /**
     * Identifikátory typů archivních entit
     * @type {Array<number>}
     * @memberof SearchFilterVO
     */
    aeTypeIds?: Array<number>;
    /**
     * Stavy achrivních entit
     * @type {Array<AeState>}
     * @memberof SearchFilterVO
     */
    aeStates?: Array<AeState>;
    /**
     * Identifikátor konkrétní archivní entity
     * @type {string}
     * @memberof SearchFilterVO
     */
    code?: string;
    /**
     *
     * @type {string}
     * @memberof SearchFilterVO
     */
    user?: string;
    /**
     * Vnik; text ve formátu datace
     * @type {string}
     * @memberof SearchFilterVO
     */
    creation?: string;
    /**
     * Zánik; text ve formátu datace
     * @type {string}
     * @memberof SearchFilterVO
     */
    extinction?: string;
    /**
     * Seznam požadovaných vztahů
     * @type {Array<RelationFilterVO>}
     * @memberof SearchFilterVO
     */
    relFilters?: Array<RelationFilterVO>;
    /**
     * Seznam rozšiřujících filtrů
     * @type {Array<ExtensionFilterVO>}
     * @memberof SearchFilterVO
     */
    extFilters?: Array<ExtensionFilterVO>;
}


