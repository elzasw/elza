import {ApPartFormVO} from "./ApPartFormVO";

export interface ApAccessPointCreateVO {
    /**
     * Identifikátor typu AP.
     */
    typeId: number;

    /**
     * Identifikátor třídy.
     */
    scopeId: number;

    /**
     * Kód jazyka jména přístupového bodu.
     */
    languageCode: string;

    /**
     * Identifikátor přístupového bodu
     */
    accessPointId?: number;

    /**
     * Formulář části
     */
    partForm: ApPartFormVO;
}
