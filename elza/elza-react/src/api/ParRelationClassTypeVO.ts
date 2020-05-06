/**
 * VO pro třídu typu vztahu.
 */
import {ParRelationClassTypeRepeatabilityEnum} from "./ParRelationClassTypeRepeatabilityEnum";

export interface ParRelationClassTypeVO {
    id: number;
    name: string;
    code: string;
    repeatability: ParRelationClassTypeRepeatabilityEnum;
}
