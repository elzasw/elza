import {ApAccessPointNameVO} from "./ApAccessPointNameVO";
import {ApStateVO} from "./ApStateVO";
import {ApFormVO} from "./ApFormVO";
import {ApExternalIdVO} from "./ApExternalIdVO";
import {ApPartVO} from "./ApPartVO";
import {StateApproval} from "./StateApproval";

/**
 * VO rejstříkového záznamu.
 */
export interface ApAccessPointVO {
    /**
     * Id hesla.
     */
    id: number;

    uuid: string;

    /**
     * Typ rejstříku.
     */
    typeId: number;

    /**
     * Id třídy rejstříku.
     */
    scopeId: number;

    /**
     * Stav schválení.
     */
    stateApproval: StateApproval;

    /**
     * Komentář ke stavu schválení.
     */
    comment: string;

    /**
     * Rejstříkové heslo.
     */
    record: string;

    /**
     * Podrobná charakteristika rejstříkového hesla.
     */
    characteristics: string;

    invalid: boolean;

    /**
     * Id osoby.
     */
    partyId: number;

    /**
     * Externí identifikátory rejstříkového hesla.
     */
    externalIds: ApExternalIdVO[];

    /**
     * Seznam jmen přístupového bodu.
     */
    names: ApAccessPointNameVO[];

    /**
     * Kód pravidla pro AP.
     */
    ruleSystemId?: number;

    /**
     * Stav přístupového bodu.
     */
    state?: ApStateVO;

    /**
     * Chyby v přístupovém bodu.
     */
    errorDescription?: string;

    /**
     * Strukturované data formuláře pro AP. Vyplněné pouze v případě, že se jedná o strukturovaný typ.
     */
    form?: ApFormVO;

    /**
     * Identifikátor preferované části
     */
    preferredPart: number;

    /**
     * Seznam částí přístupového bodu
     */
    parts: ApPartVO[];
}
