export const DEFAULT_LIST_SIZE = 200;

export enum AP_EXT_SYSTEM_TYPE {
    CAM = 'CAM',
}

export enum MODAL_DIALOG_VARIANT {
    LARGE = 'dialog-lg',
    FULLSCREEN = 'dialog-fullscreen',
    NO_HEADER = 'dialog-no-header',
}

export enum RELATION_CLASS_CODES {
    RELATION = 'R',
    BIRTH = 'B',
    EXTINCTION = 'E',
}

export enum ActionState {
    RUNNING = 'RUNNING',
    WAITING = 'WAITING',
    PLANNED = 'PLANNED',
    FINISHED = 'FINISHED',
    ERROR = 'ERROR',
    INTERRUPTED = 'INTERRUPTED',
    OUTDATED = 'OUTDATED',
}

export enum ApState {
    OK = 'OK',
    ERROR = 'ERROR',
    TEMP = 'TEMP',
    INIT = 'INIT',
}

export const FOCUS_KEYS = {
    NONE: null,
    ARR: 'arr',
    PARTY: 'party',
    REGISTRY: 'registry',
    HOME: 'home',
    FUND_OUTPUT: 'fund-output',
    FUND_ACTION: 'fund-action',
    FUND_REQUEST: 'fund-request',
    ADMIN_EXT_SYSTEM: 'admin-extSystem',
};

/**
 * Formát pro zobrazení typu Integer.
 * @type {{NUMBER: string, DURATION: string}}
 */
export enum DisplayType {
    NUMBER = 'NUMBER',
    DURATION = 'DURATION',
}

//konkrétrní kód akce pro perzistentní řazení v balíčku ZP2015
export const PERSISTENT_SORT_CODE = 'PERZISTENTNI_RAZENI';
export const ZP2015_INTRO_VYPOCET_EJ = 'ZP2015_INTRO_VYPOCET_EJ';

export const ELZA_SCHEME_NODE = 'elza-node';

// Migrated from CAM

// const appWindow = window as any;
// const appConfig = appWindow.appConfig;
// let baseUrl = 'http://localhost:3000';
//
// if (appConfig) {
//     baseUrl = window.location.origin;
//
//     if (appConfig.serverContextPath) {
//         baseUrl += appConfig.serverContextPath;
//     }
// }
//
//
// export const BASE_API_URL =  baseUrl;

export const AUTH_INFO = 'authInfo';

export const GLOBAL_AE_LIST_AREA = 'globalAeList';
export const GLOBAL_AE_DETAIL_AREA = 'globalAeDetail';
export const GLOBAL_EDIT_AE_DETAIL_AREA = 'globalEditAeDetail';
export const GLOBAL_APPROVE_AE_DETAIL_AREA = 'globalApproveAeDetail';

export const EDIT_AE_LIST_AREA = 'editAeList';
export const EDIT_AE_DETAIL_AREA = 'editAeDetail';
export const EDIT_GLOBAL_AE_DETAIL_AREA = 'editGlobalAeDetail';

export const APPROVE_AE_LIST_AREA = 'approveAeList';
export const APPROVE_AE_DETAIL_AREA = 'approveAeDetail';
export const APPROVE_GLOBAL_AE_DETAIL_AREA = 'approveGlobalAeDetail';

export const PART_EDIT_FORM_ATTRIBUTES = 'partEditFormAttributes';

export const REGISTRY_DETAIL_HISTORY = 'registryDetailHistory';

export const AP_VALIDATION = 'apValidation';
export const AP_VIEW_SETTINGS = 'apViewSettings';

export const ADMIN_USER = 'admin';

export const FORM_DATA_GRID_EXPORT = 'dataGridExportForm';

export const JAVA_CLASS_AP_ACCESS_POINT_VO = 'cz.tacr.elza.controller.vo.ApAccessPointVO';
export const JAVA_CLASS_ARR_DIGITIZATION_FRONTDESK_SIMPLE_VO = '.ArrDigitizationFrontdeskSimpleVO';
export const JAVA_ATTR_CLASS = '@class';
