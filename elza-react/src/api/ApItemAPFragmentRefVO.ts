import {ApItemVO} from "./ApItemVO";
import {ApFragmentVO} from "./ApFragmentVO";

export interface ApItemAPFragmentRefVO extends ApItemVO {
    /**
     * fragment
     */
    fragment: ApFragmentVO;
    value: number;
}
