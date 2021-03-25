import { ArrFundBaseVO } from "../../api/ArrFundBaseVO";
import { RefTablesState } from "./RefTables.types";
import { UserDetail } from "./UserDetail.types";

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

export interface Node {
    routingKey?: string;
}

export interface NodesState {
    activeIndex: number | unknown;
    nodes: Node[];
}

export interface Fund {
    activeVersion: unknown;
    apScopes: unknown;
    bulkActions: unknown;
    closed: boolean | unknown;
    createDate: string | unknown;
    daoPackageDetail: unknown;
    daoPackageList: unknown;
    daoUnassignedPackageList: unknown;
    dirty: boolean | unknown;
    fundAction: unknown;
    fundDataGrid: unknown;
    fundFiles: unknown;
    fundNodesError: unknown;
    fundNodesPolicy: unknown;
    fundNumber: unknown | null;
    fundOutput: unknown;
    fundTree: unknown;
    fundTreeDaosLeft: unknown;
    fundTreeDaosRight: unknown;
    fundTreeMovementsLeft: unknown;
    fundTreeMovementsRight: unknown;
    fundTreeNodes: unknown;
    historicalNamedOutputs: unknown | null;
    id: number | unknown;
    institutionId: number | unknown;
    internalCode: string | unknown;
    isFetching: boolean | unknown;
    lastUseTemplateName?: unknown;
    lockDate: unknown | null;
    mark: unknown | null;
    moving: boolean | unknown;
    name: string | unknown;
    nodeDaoList: unknown;
    nodeDaoListAssign: unknown;
    nodes: NodesState;
    packageDaoList: unknown;
    reducer: unknown;
    requestDetail: unknown;
    requestList: unknown;
    unitdate: unknown | null;
    validNamedOutputs: unknown | null;
    versionId: number | unknown;
    versionValidation: unknown;
    versions: unknown;
}

export interface VisiblePolicy {
    data: null | unknown;
    otherData: null | unknown;
}

export interface ArrRegion {
    activeIndex: number | null;
    customFund: unknown;
    extendedView?: boolean;
    fundSearch: unknown;
    funds: Fund[];
    globalFundTree: unknown;
    nodeSettings: unknown;
    showRegisterJp?: boolean;
    visiblePolicy: VisiblePolicy;
}


export interface AppState {
    splitter: SplitterState;
    adminRegion: AdminRegionState;
    arrRegion: ArrRegion;
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
    userDetail: UserDetail;
    webSocket: unknown;
}
