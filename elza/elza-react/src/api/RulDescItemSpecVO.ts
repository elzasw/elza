import {BaseCodeVo} from "./BaseCodeVo";
import {RulItemSpecType} from "./RulItemSpecType";

/**
 * VO specifikace atributu.
 */
export interface RulDescItemSpecVO extends BaseCodeVo {
    /**
     * zkratka
     */
    shortcut: string;

    /**
     * popis
     */
    description: string;

    /**
     * řazení
     */
    viewOrder: number;

    /**
     * typ důležitosti
     * @deprecated
     */
    type: RulItemSpecType;

    /**
     * opakovatelnost
     * @deprecated
     */
    repeatable: boolean;
}
