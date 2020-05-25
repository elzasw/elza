import {BaseCodeVo} from "./BaseCodeVo";

/**
 * VO datového typu.
 */
export interface RulDataTypeVO extends BaseCodeVo {
    /**
     * popis
     */
    description: string;

    /**
     * lze použít regulární výraz?
     */
    regexpUse: boolean;

    /**
     * lze použít limitaci délky textu?
     */
    textLengthLimitUse: boolean;

    /**
     * tabulka pro uložení
     */
    storageTable: string;
}
