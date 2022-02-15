import * as permissions from 'actions/user/Permission';
export enum AuthType {
    PASSWORD = "PASSWORD"
}

    /*
export enum PermissionType {
    ADMIN = "ADMIN",
    FUND_RD = "FUND_RD",
    FUND_RD_ALL = "FUND_RD_ALL",
    FUND_ARR = "FUND_ARR",
    FUND_ARR_ALL = "FUND_ARR_ALL",
    AP_SCOPE_RD = "AP_SCOPE_RD",
    AP_SCOPE_RD_ALL = "AP_SCOPE_RD_ALL",
    AP_SCOPE_WR = "AP_SCOPE_WR",
    AP_SCOPE_WR_ALL = "AP_SCOPE_WR_ALL",
    AP_CONFIRM = "AP_CONFIRM",
    AP_CONFIRM_ALL = "AP_CONFIRM_ALL",
    AP_EDIT_CONFIRMED = "AP_EDIT_CONFIRMED",
    AP_EDIT_CONFIRMED_ALL = "AP_EDIT_CONFIRMED_ALL",
    FUND_OUTPUT_WR = "FUND_OUTPUT_WR",
    FUND_OUTPUT_WR_ALL = "FUND_OUTPUT_WR_ALL",
    FUND_VER_WR = "FUND_VER_WR",
    FUND_ADMIN = "FUND_ADMIN",
    FUND_CREATE = "FUND_CREATE",
    FUND_EXPORT = "FUND_EXPORT",
    FUND_EXPORT_ALL = "FUND_EXPORT_ALL",
    FUND_ISSUE_LIST_RD = "FUND_ISSUE_LIST_RD",
    FUND_ISSUE_LIST_WR = "FUND_ISSUE_LIST_WR",
    FUND_ISSUE_ADMIN = "FUND_ISSUE_ADMIN",
    FUND_ISSUE_ADMIN_ALL = "FUND_ISSUE_ADMIN_ALL",
    AP_EXTERNAL_WR = "AP_EXTERNAL_WR",
    USR_PERM = "USR_PERM",
    FUND_BA = "FUND_BA",
    FUND_BA_ALL = "FUND_BA_ALL",
    FUND_CL_VER_WR = "FUND_CL_VER_WR",
    FUND_CL_VER_WR_ALL = "FUND_CL_VER_WR_ALL",
    USER_CONTROL_ENTITITY = "USER_CONTROL_ENTITITY",
    GROUP_CONTROL_ENTITITY = "GROUP_CONTROL_ENTITITY",
    FUND_ARR_NODE = "FUND_ARR_NODE",
}
    */
type PermissionType = keyof typeof permissions;

export interface Permission {
    fundIds: number[];
    fundIdsMap: Record<number, boolean>;
    issueListIds: number[];
    permission: PermissionType;
    scopeIds: number[];
    scopeIdsMap: Record<number, boolean>;
}

export interface UserDetail {
    accessPoint: unknown | null;
    active: boolean;
    authTypes: AuthType[];
    description: unknown | null;
    fetched: boolean;
    fetching: boolean;
    groups: unknown | null;
    hasAll: () => unknown;
    hasArrOutputPage: () => unknown;
    hasArrPage: () => unknown;
    hasFundActionPage: () => unknown;
    hasOne: (permission: PermissionType) => boolean;
    hasRdPage: () => unknown;
    id: number | null;
    isAdmin: () => unknown;
    permissions: unknown | null;
    permissionsMap: Record<PermissionType, Permission>;
    settings: unknown | null;
    userPermissions: Permission[];
    username: string;
}
