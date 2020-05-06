import {ApItemVO} from "./ApItemVO";
import {ArrNodeVO} from "./ArrNodeVO";

export interface ApItemUriRefVO extends ApItemVO {
    schema: string;
    value: string;
    description: string;
    node: ArrNodeVO;
    nodeId: number;
}
