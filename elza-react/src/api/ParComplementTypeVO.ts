/**
 * VO Číselníku typů doplňků jmen osob.
 */
export interface ParComplementTypeVO {
    /**
     * Id.
     */
    complementTypeId: number;
    /**
     * Kód typu.
     */
    code: string;
    /**
     * Název typu.
     */
    name: string;

    /**
     * Pořadí zobrazení.
     */
    viewOrder: number;
}
