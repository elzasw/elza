import {PasswordPolicy} from "elza-api";
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

    /** The user must change the password before continuing. */
    needChangePassword: boolean;

    /** Password rules for client-side validation of a new password. */
    passwordPolicy?: PasswordPolicy;
}
