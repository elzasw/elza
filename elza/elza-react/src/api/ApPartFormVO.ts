import {ApItemVO} from "./ApItemVO";

export interface ApPartFormVO {
    /**
     * Kód typu části
     */
    partTypeCode: string;

    /**
     * Seznam všech hodnot atributů
     */
    items: ApItemVO[];

    /**
     * Identifikátor nadřízené části
     */
    parentPartId?: number;

    /**
    * Identifikátor nadřízené části z revize
    */
    revParentPartId?: number;
    /**
     * Identifikátor části
     */
    partId?: number;
}
