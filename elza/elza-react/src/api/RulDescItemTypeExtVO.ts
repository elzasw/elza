import {RulDescItemTypeVO} from "./RulDescItemTypeVO";
import {RulDescItemSpecExtVO} from "./RulDescItemSpecExtVO";

/**
 * VO rozšířený typu hodnoty atributu
 */
export interface RulDescItemTypeExtVO extends RulDescItemTypeVO {
    /**
     * seznam rozšířených specifikací atributu
     */
    descItemSpecs: RulDescItemSpecExtVO[];
}
