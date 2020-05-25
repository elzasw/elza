import {StateApproval} from "./StateApproval";

export interface ApStateHistoryVO {
    /**
     * Datum změny.
     */
    changeDate: string;

    /**
     * Uživatelské jméno osoby, která změnu proveda.
     */
    username:string;

    /**
     * Název oblasti.
     */
    scope:string;

    /**
     * Typ přístupového bodu.
     */
    type:string;

    /**
     * Stav změny.
     */
    state: StateApproval;

    /**
     * Komentář změny.
     */
    comment:string;
}
