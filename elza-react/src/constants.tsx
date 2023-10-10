import {string} from "prop-types";
import { Fund } from "typings/store";
import { DAO } from "components/arr/ArrUtils";

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

export const TREE = 'tree';
export const NODE = 'node';
export const GRID = 'grid';
export const MOVEMENTS = 'movements';
export const OUTPUTS = 'outputs';
export const ACTIONS = 'actions';
export const DAOS = 'daos';
export const REQUESTS = 'requests';

export const URL_ENTITY = '/entity';
export const URL_ENTITY_CREATE = '/entity-create';
export const URL_FUND = '/fund';
export const URL_FUND_TREE = `${URL_FUND}/tree`;
export const URL_FUND_GRID_PATH = `${URL_FUND}/:id/${GRID}`;
export const URL_FUND_MOVEMENTS_PATH = `${URL_FUND}/:id/${MOVEMENTS}`;
export const URL_FUND_OUTPUTS_PATH = `${URL_FUND}/:id/${OUTPUTS}/:outputId`;
export const URL_FUND_ACTIONS_PATH = `${URL_FUND}/:id/${ACTIONS}/:actionId`;
export const URL_FUND_REQUESTS_PATH = `${URL_FUND}/:id/${REQUESTS}/:requestId`;
export const URL_FUND_DAOS_PATH = `${URL_FUND}/:id/${DAOS}`;

export const URL_NODE = '/node';
export const URL_ADMIN = '/admin';
export const URL_ADMIN_USER = `${URL_ADMIN}/user`;
export const URL_ADMIN_GROUP = `${URL_ADMIN}/group`;
export const URL_ADMIN_FUND = `${URL_ADMIN}/fund`;

export const getFundVersion = (fund: Fund) => {
    if(!fund?.activeVersion){
        // console.error("No active version on fund", fund);
        // throw Error("No active version on fund")
        return undefined;
    }
    //@ts-ignore - TODO doplnit typ pro active version
    return fund?.activeVersion.lockDate === null ? undefined : fund.activeVersion.id;
}

export const urlFund = (fundId: number) => {
    return `${URL_FUND}/${fundId}`;
}

export const urlFundWithVersion = (fundId: number, versionId: number) => {
    return `${URL_FUND}/${fundId}/v/${versionId}`
}

export const urlFundBase = (fundId: number, versionId?: number) => {
    return versionId === undefined
        ? urlFund(fundId)
        : urlFundWithVersion(fundId, versionId);
}

export const urlNode = (nodeId: number | string | undefined) => {
    if (nodeId == null) {
        console.log("url fund tree")
        return URL_FUND_TREE;
    }
    return URL_NODE + "/" + nodeId;
}

const fundSub = (fundId: number, versionId: number | undefined, sub: string, subId?: number) => {
    const url = `${urlFundBase(fundId, versionId)}/${sub}`;
    return subId == null ? url : url + '/' + subId;
}

export const urlFundNode = (fundId: number, versionId?: number, nodeId?: number) => {
    return fundSub(fundId, versionId, `${NODE}/${nodeId}`);
}

export const urlFundTree = (fundId: number, versionId?: number) => {
    return fundSub(fundId, versionId, TREE);
}

export const urlFundGrid = (fundId: number, versionId?: number, filter?: string) => {
    return `${fundSub(fundId, versionId, GRID)}${filter ? "?filter="+filter : ""}`;
}

export const urlFundMovements = (fundId: number, versionId?: number) => {
    return fundSub(fundId, versionId, MOVEMENTS);
}

export const urlFundOutputs = (fundId: number, versionId?: number, outputId?: number) => {
    return fundSub(fundId, versionId, OUTPUTS, outputId);
}

export const urlFundActions = (fundId: number, versionId?: number, actionId?: number) => {
    return fundSub(fundId, versionId, ACTIONS, actionId);
}

export const urlFundRequests = (fundId: number, versionId?: number, requestId?: number) => {
    return fundSub(fundId, versionId, REQUESTS, requestId);
}

export const urlFundDaos = (fundId: number, versionId?: number, daoId?: number) => {
    return fundSub(fundId, versionId, DAOS, daoId);
}

export const urlAdminUser = (userId: number) => {
    return `${URL_ADMIN_USER}/${userId}`;
}

export const urlAdminGroup = (groupId: number) => {
    return `${URL_ADMIN_GROUP}/${groupId}`;
}

export const urlAdminFund = (fundId: number) => {
    return `${URL_ADMIN_FUND}/${fundId}`;
}

export const urlEntity = (entityId?: number | string) => {
    return `${URL_ENTITY}/${(entityId == null ? "" : entityId)}`
}

export const urlEntityRevision = (entityId?: number | string) => {
    return `${URL_ENTITY}/${(entityId == null ? "" : entityId)}/revision`
}