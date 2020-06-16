import {UsrUserVO} from "./UsrUserVO";
import {UsrPermissionVO} from "./UsrPermissionVO";

export interface UsrGroupVO {
    /** Identifikátor. */
    id: number;

    /** Kód. */
    code: string;

    /** Název. */
    name: string;

    /** Popis. */
    description: string;

    /** Oprávnění. */
    permissions: UsrPermissionVO[];

    /** Uživatelé přiřazení do skupiny. */
    users: UsrUserVO[];
}
