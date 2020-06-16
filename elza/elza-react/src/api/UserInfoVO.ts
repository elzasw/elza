import {UISettingsVO} from "./UISettingsVO";
import {UsrUserVO} from "./UsrUserVO";
import {UserPermissionInfoVO} from "./UserPermissionInfoVO";

export interface UserInfoVO extends UsrUserVO {
    /**
     * Preferred user name
     */
    preferredName: string;

    /** Oprávnění uživatele. */
    userPermissions: UserPermissionInfoVO[];

    settings: UISettingsVO[];
}
