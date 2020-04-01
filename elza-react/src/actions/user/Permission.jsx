export const ADMIN = 'ADMIN';
export const FUND_RD = 'FUND_RD';
export const FUND_RD_ALL = 'FUND_RD_ALL';
export const FUND_ARR = 'FUND_ARR';
export const FUND_ARR_ALL = 'FUND_ARR_ALL';
export const AP_SCOPE_RD = 'AP_SCOPE_RD';
export const AP_SCOPE_RD_ALL = 'AP_SCOPE_RD_ALL';
export const AP_SCOPE_WR = 'AP_SCOPE_WR';
export const AP_SCOPE_WR_ALL = 'AP_SCOPE_WR_ALL';
export const AP_CONFIRM = 'AP_CONFIRM';
export const AP_CONFIRM_ALL = 'AP_CONFIRM_ALL';
export const AP_EDIT_CONFIRMED = 'AP_EDIT_CONFIRMED';
export const AP_EDIT_CONFIRMED_ALL = 'AP_EDIT_CONFIRMED_ALL';
export const FUND_OUTPUT_WR = 'FUND_OUTPUT_WR';
export const FUND_OUTPUT_WR_ALL = 'FUND_OUTPUT_WR_ALL';
export const FUND_VER_WR = 'FUND_VER_WR';
export const FUND_ADMIN = 'FUND_ADMIN';
export const FUND_CREATE = 'FUND_CREATE';
export const FUND_EXPORT = 'FUND_EXPORT';
export const FUND_EXPORT_ALL = 'FUND_EXPORT_ALL';
export const FUND_ISSUE_LIST_RD = 'FUND_ISSUE_LIST_RD';
export const FUND_ISSUE_LIST_WR = 'FUND_ISSUE_LIST_WR';
export const FUND_ISSUE_ADMIN_ALL = 'FUND_ISSUE_ADMIN_ALL';
export const USR_PERM = 'USR_PERM';
export const FUND_BA = 'FUND_BA';
export const FUND_BA_ALL = 'FUND_BA_ALL';
export const FUND_CL_VER_WR = 'FUND_CL_VER_WR';
export const FUND_CL_VER_WR_ALL = 'FUND_CL_VER_WR_ALL';
export const USER_CONTROL_ENTITITY = 'USER_CONTROL_ENTITITY';
export const GROUP_CONTROL_ENTITITY = 'GROUP_CONTROL_ENTITITY';
export const FUND_ARR_NODE = 'FUND_ARR_NODE';

export const all = {
    ADMIN: {},
    FUND_RD: {fund: true},
    FUND_RD_ALL: {},
    FUND_ARR: {fund: true},
    FUND_ARR_ALL: {},
    AP_SCOPE_RD: {scope: true},
    AP_SCOPE_RD_ALL: {},
    AP_SCOPE_WR: {scope: true},
    AP_SCOPE_WR_ALL: {},
    AP_CONFIRM: {scope: true},
    AP_CONFIRM_ALL: {},
    AP_EDIT_CONFIRMED: {scope: true},
    AP_EDIT_CONFIRMED_ALL: {},
    FUND_OUTPUT_WR: {fund: true},
    FUND_OUTPUT_WR_ALL: {},
    FUND_VER_WR: {fund: true},
    FUND_ADMIN: {},
    FUND_CREATE: {},
    FUND_EXPORT: {fund: true},
    FUND_EXPORT_ALL: {},
    FUND_ISSUE_ADMIN_ALL: {},
    FUND_ISSUE_LIST_RD: {},
    FUND_ISSUE_LIST_WR: {},
    USR_PERM: {},
    FUND_BA: {fund: true},
    FUND_BA_ALL: {},
    FUND_CL_VER_WR: {fund: true},
    FUND_CL_VER_WR_ALL: {},
    USER_CONTROL_ENTITITY: { user: true },
    GROUP_CONTROL_ENTITITY: { group: true },
    FUND_ARR_NODE: { fund: true, node: true },
};
