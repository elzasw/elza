import { ApStateVO } from "./ApStateVO";
import { ApBindingVO } from "./ApBindingVO";
import { ApPartVO } from "./ApPartVO";
import { StateApproval } from "./StateApproval";
import { UserVO } from "./UserVO";
import { ApChangeVO } from "./ApChangeVO";
import { RevStateApproval } from "./RevStateApproval";

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
     * Komentář ke stavu schválení revize.
     */
    revComment: string;

    /**
     * Identifikátor pravidel.
     */
    ruleSetId: number;

    /**
     * Jméno přistupového bodu.
     */
    name: string;

    /**
     * Podrobný popis přístupového bodu.
     */
    description: string;

    invalid: boolean;
    replacedById?: number;
    replacedIds?: number[];

    /**
     * Externí identifikátory rejstříkového hesla.
     */
    bindings: ApBindingVO[];

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

    /**
     * Stav revize
     */
    revStateApproval: RevStateApproval;

    /**
     * Nový typ rejstříku.
     */
    newTypeId: number;

    /**
     * Identifikátor nové preferované části z revize
     */
    newPreferredPart: number;

    /**
     * Identifikátor nové preferované části, která existuje pouze v revizi
     */
    revPreferredPart: number;

    /**
     * Seznam částí přístupového bodu z revize
     */
    revParts: ApPartVO[];
}
