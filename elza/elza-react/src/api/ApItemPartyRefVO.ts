import {ApItemVO} from "./ApItemVO";
import {ParPartyVO} from "./ParPartyVO";

export interface ApItemPartyRefVO extends ApItemVO {
    party: ParPartyVO;
    value: number;
}
