import {ApAccessPointVO} from "./ApAccessPointVO";
import {ParPartyTypeVO} from "./ParPartyTypeVO";
import {ParRelationVO} from "./ParRelationVO";
import {ParPartyNameVO} from "./ParPartyNameVO";

/**
 * Abstraktní osoba.
 */
export interface ParPartyVO {
    /**
     * Id osoby.
     */
    id: number;

    /**
     * Složený název osoby
     */
    name: string;

    /**
     * Typ osoby.
     */
    partyType: ParPartyTypeVO;

    /**
     * Dějiny osoby.
     */
    history: string;
    /**
     * Zdroje informací.
     */
    sourceInformation: string;

    /**
     * Seznam vazeb osoby.
     */
    relations: ParRelationVO[];
    /**
     * Seznam jmen osoby.
     */
    partyNames: ParPartyNameVO[];
    /**
     * Seznam tvůrců osoby.
     */
    creators: ParPartyVO[];

    /**
     * Rejstříkové heslo.
     */
    accessPoint: ApAccessPointVO;

    /**
     * Charakteristika.
     */
    characteristics: string;

    /**
     * Verze záznamu.
     */
    version: number;
}
