import {ApStateVO} from "./ApStateVO";
import {ApFormVO} from "./ApFormVO";

export interface ApFragmentVO {
    /**
     * Identifikátor fragmentu.
     */
    id: number;

    /**
     * Hodnota fragmentu.
     */
    value: string;

    /**
     * Stav fragmentu.
     */
    state: ApStateVO;

    /**
     * Identifikátor typu fragmentu.
     */
    typeId: number;

    /**
     * Chyby ve fragmentu.
     */
    errorDescription?: string;

    /**
     * Strukturovaná data fragmentu.
     */
    form?: ApFormVO;

    /**
     * Identifikátor nadřazeného fragmentu.
     */
    fragmentParentId?: number;
}
