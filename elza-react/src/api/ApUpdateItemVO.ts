import {ApItemVO} from "./ApItemVO";
import {UpdateOp} from "./UpdateOp";

export interface ApUpdateItemVO {
    /**
     * Typ změny.
     */
    updateOp: UpdateOp;

    /**
     * Položka změny.
     */
    item: ApItemVO;
}
