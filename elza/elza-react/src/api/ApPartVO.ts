import {ApStateVO} from "./ApStateVO";
import {ApItemVO} from "./ApItemVO";

export interface ApPartVO {
    /**
     * Identifikátor partu.
     */
    id: number;

    /**
     * Hodnota partu.
     */
    value: string;

    /**
     * Stav partu.
     */
    state: ApStateVO;

    /**
     * Identifikátor typu partu.
     */
    typeId: number;

    /**
     * Chyby v partu.
     */
    errorDescription?: string;

    /**
     * Identifikátor nadřazeného partu.
     */
    partParentId?: number;

    /**
     * Seznam hodnot atributů
     */
    items: ApItemVO[] | null;

    /**
     * Identifikátor původního partu
     */
    origPartId?: number;
    /**
    * Typ zmeny v revizi
    */
    changeType?: "DELETED" | "NEW" | "UPDATED";
}
