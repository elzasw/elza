import {Permission} from "./Permission";

export interface UserPermissionInfoVO {
    /** Typ oprávnění. */
    permission: Permission;

    /** Seznam identifikátorů AS, ke kterým se vztahuje oprávnění. */
    fundIds: number[];

    /** Seznam identifikátorů scopů, ke kterým se vztahuje oprávnění. */
    scopeIds: number[];

    /**
     * Seznam identifikátorů protokolů, ke kterým se vztahuje oprávnění.
     */
    issueListIds: number[];
}
