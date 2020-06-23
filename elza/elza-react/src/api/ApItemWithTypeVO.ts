import {ApItemVO} from "./ApItemVO";
import {RulDescItemTypeExtVO} from "./RulDescItemTypeExtVO";

export interface ApItemWithTypeVO extends ApItemVO {
    /**
     * typ
     */
    type: RulDescItemTypeExtVO | null;
}
