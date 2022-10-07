import {string} from "prop-types";

export const DEFAULT_LIST_SIZE = 200;

export enum AP_EXT_SYSTEM_TYPE {
    CAM = 'CAM',
    CAM_UUID = 'CAM_UUID',
    CAM_COMPLETE = 'CAM_COMPLETE',
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

export enum ItemClass {
    BIT = ".ArrItemBitVO",
    COORDINATES = ".ArrItemCoordinatesVO",
    DATE = ".ArrItemDateVO",
    DECIMAL = ".ArrItemDecimalVO",
    ENUM = ".ArrItemEnumVO",
    FILE_REF = ".ArrItemFileRefVO",
    FORMATTED_TEXT = ".ArrItemFormattedTextVO",
    INT = ".ArrItemIntVO",
    JSON_TABLE = ".ArrItemJsonTableVO",
    RECORD_REF = ".ArrItemRecordRefVO",
    STRING = ".ArrItemStringVO",
    STRUCTURE = ".ArrItemStructureVO",
    TEXT = ".ArrItemTextVO",
    UNITDATE = ".ArrItemUnitdateVO",
    UNIT_ID = ".ArrItemUnitidVO",
    URI_REF = ".ArrItemUriRefVO",
}

export enum MODAL_DIALOG_SIZE {
    FULLSCREEN = 'dialog-fullscreen',
    LG = 'dialog-lg',
    XL = 'dialog-xl',
    SM = 'dialog-sm',
}

export enum CoordinateFileType {
    KML = 'KML',
    GML = 'GML',
    WKT = 'WKT',
}

export const CLS_CALCULABLE = 'calculable';

const GRID = 'grid';
const MOVEMENTS = 'movements';
const OUTPUTS = 'outputs';
const ACTIONS = 'actions';
const DAOS = 'daos';
const REQUESTS = 'requests';

export const REG_NUMBER = "[0-9]+";
export const REG_UUID = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

export const URL_ENTITY = '/entity';
export const URL_ENTITY_CREATE = '/entity-create';
export const URL_FUND = '/fund';
export const URL_FUND_TREE = URL_FUND + '/tree';
export const URL_FUND_GRID_PATH = URL_FUND + "/:id(" + REG_NUMBER + ")/" + GRID;
export const URL_FUND_MOVEMENTS_PATH = URL_FUND + "/:id(" + REG_NUMBER + ")/" + MOVEMENTS;
export const URL_FUND_OUTPUTS_PATH = URL_FUND + "/:id(" + REG_NUMBER + ")/" + OUTPUTS + "/:outputId([0-9]+)?";
export const URL_FUND_ACTIONS_PATH = URL_FUND + "/:id(" + REG_NUMBER + ")/" + ACTIONS + "/:actionId([0-9]+)?";
export const URL_FUND_REQUESTS_PATH = URL_FUND + "/:id(" + REG_NUMBER + ")/" + REQUESTS + "/:requestId([0-9]+)?";
export const URL_FUND_DAOS_PATH = URL_FUND + "/:id(" + REG_NUMBER + ")/" + DAOS;

export const URL_NODE = '/node'

export const urlFund = (fundId: number) => {
    return URL_FUND + "/" + fundId;
}

export const urlNode = (nodeId: number | undefined) => {
    if (nodeId == null) {
        return URL_FUND_TREE;
    }
    return URL_NODE + "/" + nodeId;
}

const fundSub = (fundId: number, sub: string, subId?: number) => {
    const url = urlFund(fundId) + '/' + sub
    return subId == null ? url : url + '/' + subId;
}

export const urlFundGrid = (fundId: number) => {
    return fundSub(fundId, GRID);
}

export const urlFundMovements = (fundId: number) => {
    return fundSub(fundId, MOVEMENTS);
}

export const urlFundOutputs = (fundId: number, outputId?: number) => {
    return fundSub(fundId, OUTPUTS, outputId);
}

export const urlFundActions = (fundId: number, actionId?: number) => {
    return fundSub(fundId, ACTIONS, actionId);
}

export const urlFundRequests = (fundId: number, requestId?: number) => {
    return fundSub(fundId, REQUESTS, requestId);
}

export const urlFundDaos = (fundId: number, daoId?: number) => {
    return fundSub(fundId, DAOS, daoId);
}
