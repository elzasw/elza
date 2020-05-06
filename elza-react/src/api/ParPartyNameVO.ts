import {ParUnitdateVO} from "./ParUnitdateVO";
import {ParPartyNameFormTypeVO} from "./ParPartyNameFormTypeVO";
import {ParPartyNameComplementVO} from "./ParPartyNameComplementVO";

/**
 * Jméno abstraktní osoby.
 */
export interface ParPartyNameVO {
    /**
     * Vlastní ID.
     */
    id: number;

    /**
     * Platnost jména od.
     */
    validFrom: ParUnitdateVO;
    /**
     * Platnost jména do.
     */
    validTo: ParUnitdateVO;

    /**
     * Typ jména.
     */
    nameFormType: ParPartyNameFormTypeVO;

    /**
     * Id osoby.
     */
    partyId: number;

    /**
     * Seznam doplňků jména.
     */
    partyNameComplements: ParPartyNameComplementVO[];
    /**
     * Hlavní část jména.
     */
    mainPart: string;
    /**
     * Vedlejší část jména.
     */
    otherPart: string;
    /**
     * Poznámka - využije se v případě nutnosti doplnit informaci uvedenou v prvcích.
     */
    note: string;

    /**
     * Titul před jménem.
     */
    degreeBefore: string;

    /**
     * Titul za jménem.
     */
    degreeAfter: string;

    /**
     * Poskládané jméno pro zobrazení.
     */
    displayName: string;

    prefferedName: boolean;
}
