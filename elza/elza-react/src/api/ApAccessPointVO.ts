import {ApStateVO} from "./ApStateVO";
import {ApExternalIdVO} from "./ApExternalIdVO";
import {ApPartVO} from "./ApPartVO";
import {StateApproval} from "./StateApproval";
import {UserVO} from "./UserVO";
import {ApChangeVO} from "./ApChangeVO";

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
     * Jméno přistupového bodu.
     */
    name: string;

    /**
     * Podrobný popis přístupového bodu.
     */
    description: string;

    invalid: boolean;

    /**
     * Externí identifikátory rejstříkového hesla.
     */
    externalIds: ApExternalIdVO[];

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
     * Identifikátor preferované části
     */
    preferredPart: number;

    /**
     * Seznam částí přístupového bodu
     */
    parts: ApPartVO[];

    /**
     * Poslední změna přístupového bodu
     */
    lastChange: ApChangeVO;

    /**
     * Vlastník přístupového bodu
     */
    ownerUser: UserVO;

    /**
     * Počet komentářů
     */
    comments: number;
}
