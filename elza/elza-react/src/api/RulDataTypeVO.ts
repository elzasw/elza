import {BaseCodeVo} from "./BaseCodeVo";
import {RulDataTypeCodeEnum} from "./RulDataTypeCodeEnum";

/**
 * VO datového typu.
 */
export interface RulDataTypeVO extends BaseCodeVo {
    /**
     * kod
     */
    code: RulDataTypeCodeEnum;

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
