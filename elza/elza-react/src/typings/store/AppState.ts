import { RulDescItemTypeVO } from '../../api/RulDescItemTypeVO';
import { ArrFundBaseVO } from '../../api/ArrFundBaseVO';
import { BaseRefTableStore } from '../BaseRefTableStore';

export interface SplitterState {
    leftWidth: number;
    rightWidth: number;
}

export interface ContextMenuState {
    component: unknown;
    menu: unknown;
    position: unknown;
    visible: boolean;
}

export interface DeveloperState {
    enabled: boolean;
}

export interface FocusState {
    area: number;
    component: "tree" | unknown;
    item: unknown;
    region: "arr" | unknown;
}

export interface LoginState {
    logged: boolean;
}

/*
interface RefTableArr<T> {
    items: T[];
    itemsMap: Record<number, T>;
    dirty: boolean;
    fetched: boolean;
    isFetching: boolean;
}
*/

export interface CalendarType {
    code: string;
    id: number;
    name: string;
}

export interface RefTablesState {
    apTypes: BaseRefTableStore<unknown>;
    calendarTypes: BaseRefTableStore<CalendarType>;
    descItemTypes: BaseRefTableStore<RulDescItemTypeVO>;
    eidTypes: unknown;
    externalSystems: unknown;
    groups: unknown;
    institutions: unknown;
    issueStates: unknown;
    issueTypes: unknown;
    outputTypes: unknown;
    partTypes: unknown;
    recordTypes: unknown;
    rulDataTypes: unknown;
    ruleSet: unknown;
    scopesData: unknown;
    structureTypes: unknown;
    templates: unknown;
    visiblePolicyTypes: unknown;
}

export interface AdminFulltext {
    fetched?: boolean;
    indexing?: boolean;
    isFetching?: boolean;
}

export interface AdminRegionState {
    entityPermissions: unknown;
    fulltext: AdminFulltext;
    fund: AdminFund;
    funds: AdminFunds;
    group: unknown;
    groupsPermissionsByFund: unknown;
    packages: unknown;
    user: unknown;
    usersPermissionsByFund: unknown;
}

interface FundData {
}

export interface AdminFundsFilter {
    from?: number;
    pageSize?: number;
    text?: string;
}

export interface AdminFunds {
    count?: number;
    currentDataKey?: string | number;
    filter?: AdminFundsFilter;
    filterRows?: unknown;
    getDataKey?: () => number | string;
    fetched?: boolean;
    isFetching?: boolean;
    reducer?: unknown;
    filteredRows?: ArrFundBaseVO[];
    rows?: ArrFundBaseVO[];
    sourceRows?: ArrFundBaseVO[];
}

export interface AdminFund {
    currentDataKey?: number | string;
    data?: FundData | null;
    getDataKey?: () => number | string;
    id?: number;
    fetched?: boolean;
    isFetching?: boolean;
    reducer: unknown;
}

export interface AppState {
    splitter: SplitterState;
    adminRegion: AdminRegionState;
    arrRegion: unknown;
    app: unknown;
    contextMenu: ContextMenuState;
    developer: DeveloperState;
    focus: FocusState;
    form: unknown;
    fundRegion: unknown;
    login: LoginState;
    modalDialog: unknown;
    refTables: RefTablesState;
    router: unknown;
    stateRegion: unknown;
    status: unknown;
    structures: unknown;
    tab: unknown;
    toastr: unknown;
    userDetail: unknown;
    webSocket: unknown;
}
