import { ArrFundBaseVO } from "../../api/ArrFundBaseVO";
import { RefTablesState } from "./RefTables.types";
import { UserDetail } from "./UserDetail.types";
import { ModalDialogState } from "./ModalDialog.types";
import { DetailStoreState } from "types";
import { ApValidationErrorsVO } from "api/ApValidationErrorsVO";
import { SubNodeForm } from "./SubNodeForm.types";
import { FundOutput } from "./Outputs.types";
import { FundDataGrid } from "./DataGrid.types";

import { ApAccessPointVO } from "api/ApAccessPointVO.ts";
import { AbstractFilter, AipDetailVO, AipFieldName, Sorting } from "elza-api";
import { MultiFilterObject } from "components/arr/search-funds-form/filters/types";
import { ArrDaoVO } from "typings/dao";

export interface SplitterSizes {
    leftWidth: number;
    rightWidth: number;
}

export interface SplitterState {
    /** Rozmery panelu podle oblasti (global, AIP, DAO, ARR, ...). */
    splitters: Record<string, SplitterSizes>;
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
    funds: SimpleList<ArrFundBaseVO>;
    group: unknown;
    groupsPermissionsByFund: unknown;
    packages: unknown;
    user: unknown;
    usersPermissionsByFund: unknown;
}

interface FundData {
}

export interface SimpleListFilter {
    from?: number;
    pageSize?: number;
    text?: string;
}

export interface SimpleList<T> {
    count?: number;
    currentDataKey?: string | number;
    filter?: SimpleListFilter;
    filterRows?: unknown;
    getDataKey?: () => number | string;
    fetched?: boolean;
    isFetching?: boolean;
    reducer?: unknown;
    filteredRows: T[];
    rows: T[];
    sourceRows: T[];
}

export interface ApAccessPointFilter extends SimpleListFilter { }

export interface ApAccessPoints {
    count?: number;
    currentDataKey?: string | number;
    filter?: ApAccessPointFilter;
    filterRows?: unknown;
    getDataKey?: () => number | string;
    fetched?: boolean;
    isFetching?: boolean;
    reducer?: unknown;
    filteredRows?: ApAccessPointVO[];
    rows?: ApAccessPointVO[];
    sourceRows?: ApAccessPointVO[];
}

export interface AdminFundsFilter extends SimpleListFilter { }

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
    id?: number | string;
    fetched?: boolean;
    isFetching?: boolean;
    reducer?: unknown;
    name?: string;
}
/**
 * One condition of the AIP list as the screen holds it.
 *
 * `filter` is sent to the server as it stands; the rest is what the screen needs to show and
 * remove the condition, and deliberately stays out of the shared contract.
 */
export type AipFilterEntry = {
    id: string;
    field: AipFieldName;
    filter: AbstractFilter;
    /** Text shown on the tag when the value itself is not readable, e.g. an entity id. */
    label?: string;
    /** Applied by the screen rather than the user, so it is not shown or removable. */
    invisible?: boolean;
}
export interface AipsFilter extends SimpleListFilter {
    filters?: AipFilterEntry[];
    sort?: Sorting[];
}

export interface Aips {
    count?: number;
    currentDataKey?: string | number;
    filter?: AipsFilter;
    filterRows?: unknown;
    getDataKey?: () => number | string;
    fetched?: boolean;
    isFetching?: boolean;
    reducer?: unknown;
    filteredRows?: AipDetailVO[];
    rows?: AipDetailVO[];
    sourceRows?: AipDetailVO[];
}

export interface Aip {
    currentDataKey?: number | string;
    data?: AipDetailVO | null;
    getDataKey?: () => number | string;
    id?: number | string;
    fetched?: boolean;
    isFetching?: boolean;
    reducer?: unknown;
    name?: string;
}

export interface SubNode {
    accordionLeft?: string;
    accordionRight?: string;
    digitizationRequests?: unknown | null;
    id?: number;
    issues?: unknown[];
    nodeConformity?: unknown;
    referenceMark?: string[];
    version?: number;
}

export interface NodeBase {
    arrPerm?: boolean;
    depth?: number;
    hasChildren?: boolean;
    icon?: string;
    id?: number;
    name?: string;
    referenceMark?: string[];
    version?: number;
}

export interface Node extends NodeBase {
    changeParent?: boolean;
    childNodes?: unknown[];
    developerScenarios: unknown;
    dirty?: boolean;
    filterText?: string;
    isFetching?: boolean;
    isNodeInfoFetching?: boolean;
    lastUpdated?: number;
    nodeCount: number;
    nodeIndex: number;
    nodeInfoDirty?: boolean;
    nodeInfoFetched?: boolean;
    pageSize: number;
    parentNodes?: unknown[];
    routingKey?: string;
    searchedIds?: unknown;
    selectedSubNodeId?: number;
    subNodeDaos?: unknown;
    subNodeForm?: SubNodeForm;
    subNodeInfo?: unknown;
    viewStartIndex: number;
}

export interface NodesState {
    activeIndex?: number | null;
    nodes: Node[];
}

export interface FundTree {
    expandedIds: unknown[]
    fetched?: boolean
    isFetching?: boolean
    dirty?: boolean
    nodes?: unknown[]
}

export interface Fund {
    activeVersion: ActiveVersion;
    apScopes: unknown;
    bulkActions: unknown;
    closed: boolean | unknown;
    createDate: string | unknown;
    daoPackageDetail: unknown;
    daoPackageList: unknown;
    daoUnassignedPackageList: unknown;
    dirty: boolean | unknown;
    fundAction: unknown;
    fundDataGrid: FundDataGrid;
    fundFiles: unknown;
    fundNodesError: unknown;
    fundNodesPolicy: unknown;
    fundNumber: unknown | null;
    fundOutput: FundOutput;
    fundTree: FundTree;
    fundTreeDaosLeft: unknown;
    fundTreeDaosRight: unknown;
    fundTreeMovementsLeft: unknown;
    fundTreeMovementsRight: unknown;
    fundTreeNodes: unknown;
    id?: number;
    institutionId: number | unknown;
    internalCode: string | unknown;
    isFetching: boolean | unknown;
    lastUseTemplateName?: unknown;
    lockDate: unknown | null;
    mark: unknown | null;
    moving: boolean | unknown;
    name: string | unknown;
    nodeDaoList: SimpleList<ArrDaoVO>;
    nodeDaoListAssign: SimpleList<ArrDaoVO>;
    nodes: NodesState;
    packageDaoList: SimpleList<ArrDaoVO>;
    reducer: unknown;
    requestDetail: unknown;
    requestList: unknown;
    unitdate: unknown | null;
    validNamedOutputs: unknown | null;
    versionId: number;
    versionValidation: unknown;
    versions: unknown;
}

export interface Extension {
    id: number;
    code: string;
    name: string;
}

export interface VisiblePolicyOtherData {
    nodePolicyTypeIdsMap: Record<number, boolean>
    policyTypeIdsMap: Record<number, boolean>
    availableExtensions: Extension[],
    parentExtensions: Extension[],
    nodeExtensions: Extension[],
}

export interface VisiblePolicyDataItem {
    id: number;
    checked: boolean;
}

export interface VisiblePolicy {
    // data: VisiblePolicyDataItem[] | null;
    otherData: VisiblePolicyOtherData | null;
    fetched: boolean;
    fetching: boolean;
}

export interface FundSearchNodeType {
    arrPerm?: boolean;
    depth: number;
    hasChildren: boolean;
    icon?: string;
    id: number;
    name: string;
    referenceMark: string[];
    version: number;
}

export interface FundSearchFundType {
    count: number;
    expanded?: boolean;
    fetched?: boolean;
    isFetching?: boolean;
    fundVersionId: number;
    icon?: string;
    id: number;
    internalCode?: string | null;
    name: string;
    nodes?: FundSearchNodeType[];
}

export interface FundSearch {
    fulltext: string;
    filters: MultiFilterObject[];
    funds: FundSearchFundType[];
    fetched: boolean;
    isFetching: boolean;
    isIdSearch: boolean;
    partialResult: boolean;
    totalCount: number;
}

export interface NodeSettings {
  copyAll: boolean;
  descItemTypeCopyIds: number[];
  descItemTypeLockIds: number[];
  id: number;
}

export interface NodesSettings {
  nodes: NodeSettings[];
}

export interface ArrRegion {
    activeIndex: number | null;
    customFund: unknown;
    extendedView?: boolean;
    fundSearch: FundSearch;
    funds: Fund[];
    globalFundTree: unknown;
    nodeSettings: NodesSettings;
    showRegisterJp?: boolean;
    visiblePolicy: VisiblePolicy;
}

export interface ApExternalSystemSimpleVO {
    code?: string;
    id?: number;
    name?: string;
    type?: string;
    scope?: number | null;
}

export interface ExternalSystem {
    "@class": string;
    id?: number;
    code?: string;
    name?: string;
    type?: string;
    url?: string;
    apiKeyId?: string;
    apiKeyValue?: string;
    username?: string;
    password?: string;
    elzaCode?: string;
    publishOnlyApproved?: boolean;
    userInfo?: unknown;
    viewFileUrl?: string;
    viewThumbnailUrl?: string;
    sendNotification?: boolean;
    multipleLinks?: boolean;
    downloadMethod?: string;
    onReceived?: string;
    syncDelay?: number;
}

type KMLExternalSystem = Omit<ExternalSystem, "username" | "password" | "elzaCode" | "publishOnlyApproved" | "userInfo" | "viewFileUrl" | "viewThumbnailUrl" | "sendNotification">;

export interface RegistryDetail extends ItemDetail<ApAccessPointVO>{
    coordinatesInternalId?: number;
    variantRecordInternalId?: number;
}

export interface ItemDetail<T> {
    currentDataKey?: number | string;
    data?: T;
    fetched?: boolean;
    isFetching?: boolean;
    getDataKey?: () => unknown;
    id?: number;
    reducer?: unknown;
}

export interface App {
    aip: Aip;
    aipList: SimpleList<AipDetailVO>;
    aipStricture: DetailStoreState<any>;
    daoList: SimpleList<any>; //TODO: @kasparova
    explorerItem: DetailStoreState<any> //TODO: @kasparova
    accessPoins:SimpleList<ApAccessPointVO>
    apExtSystemList: SimpleList<ApExternalSystemSimpleVO>;
    apValidation: DetailStoreState<ApValidationErrorsVO>;
    apViewSettings: unknown;
    arrStructure: unknown;
    extSystemDetail: ItemDetail<ExternalSystem>;
    extSystemList: SimpleList<ExternalSystem>;
    issueComments: unknown;
    issueDetail: unknown;
    issueList: unknown;
    issueProtocol: unknown;
    issueProtocols: unknown;
    issueProtocolsConfig: unknown;
    kmlExtSystemList: SimpleList<KMLExternalSystem>;
    languageList: unknown;
    mimeTypesList: unknown;
    preparedRequestList: unknown;
    registryDetail: RegistryDetail;
    registryDetailHistory: unknown;
    registryLayerList: unknown;
    registryList: unknown;
    requestInQueueList: unknown;
    scopeDetail: unknown;
    scopeList: unknown;
    shared: unknown;
}

export interface ActiveVersion {
    config?: unknown;
    createDate?: string;
    dateRange?: unknown;
    id: number;
    issues: unknown[];
    lockDate?: string | null;
    packageId?: unknown | null;
    ruleSetId?: number;
    strictMode?: boolean;
}

export interface ArrRegionFrontFund {
    id: number;
    lockdate?: string | null;
    lastUseTemplateName?: unknown;
    name: string;
    versionId: number;
    activeVersion?: ActiveVersion;
    fundDataGrid?: unknown;
    fundFiles?: unknown;
    fundOutput?: unknown;
    fundTree: unknown;
    fundTreeDaosLeft: unknown;
    fundTreeDaosRight: unknown;
    fundTreeMovementsLeft: unknown;
    fundTreeMovementsRight: unknown;
    nodes: unknown;
}

export interface RegistryRegionFrontEntity {
    id: number;
    data: ApAccessPointVO;
}

export interface StateRegion {
    adminRegion: unknown;
    app: unknown;
    arrRegion: unknown;
    arrRegionFront: ArrRegionFrontFund[];
    fundRegion: unknown;
    registryRegionFront: RegistryRegionFrontEntity[];
}

export interface StructureNodeFormState {
    id: number | null;
    fetched: boolean;
    fetching: boolean;
    currentDataKey: unknown;
    subNodeForm: unknown;
    version?: number;
}

export interface StructuresState {
    stores: {
        [key: string]: StructureNodeFormState;
    };
}

export interface WebSocketState {
    connected: boolean;
    loading: boolean;
    disconnectedOnError: boolean;
}

export interface AppState {
    splitter: SplitterState;
    adminRegion: AdminRegionState;
    arrRegion: ArrRegion;
    app: App;
    contextMenu: ContextMenuState;
    developer: DeveloperState;
    focus: FocusState;
    form: unknown;
    fundRegion: unknown;
    login: LoginState;
    modalDialog: ModalDialogState;
    refTables: RefTablesState;
    router: unknown;
    stateRegion: StateRegion;
    status: unknown;
    structures: StructuresState;
    tab: unknown;
    toastr: unknown;
    userDetail: UserDetail;
    webSocket: WebSocketState;
}
