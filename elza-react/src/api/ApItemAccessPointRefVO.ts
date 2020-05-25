import {ApAccessPointVO} from "./ApAccessPointVO";
import {ApItemVO} from "./ApItemVO";

export interface ApItemAccessPointRefVO extends ApItemVO {
    accessPoint: ApAccessPointVO;
    value: number;
}
