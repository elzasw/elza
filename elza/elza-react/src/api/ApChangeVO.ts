import {UserVO} from "./UserVO";

export interface ApChangeVO {
    /**
     * Identifikátor
     */
    id: number;

    /**
     * Časová značka změny
     */
    change: string;

    /**
     * Uživatel, který proveld změnu
     */
    user: UserVO;
}
