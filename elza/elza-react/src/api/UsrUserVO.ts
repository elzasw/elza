import {ApAccessPointVO} from "./ApAccessPointVO";
import {UsrGroupVO} from "../types";
import {UsrPermissionVO} from "./UsrPermissionVO";
import {AuthType} from "./AuthType";

export interface UsrUserVO {
    /** Uživatelské jméno. */
    username: string;

    /** Identifikátor uživatele. */
    id: number;

    /** Je aktivní. */
    active: boolean;

    /** Popis. */
    description: string;

    /** Přístupový bod. */
    accessPoint: ApAccessPointVO;

    /**
     * Oprávnění.
     */
    permissions: UsrPermissionVO[];

    /** Seznam skupin. */
    groups: UsrGroupVO[];

    authTypes: AuthType[];
}
