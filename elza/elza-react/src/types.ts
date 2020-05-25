import {ApTypeVO} from "./api/ApTypeVO";
import {RulDataTypeVO} from "./api/RulDataTypeVO";
import {RulDescItemTypeExtVO} from "./api/RulDescItemTypeExtVO";
import {RulPartTypeVO} from "./api/RulPartTypeVO";

export interface IssueListVO {
    id: number;
    fundId: number;
    name: string;
    open: boolean;
    rdUsers: UsrUserVO[];
    wrUsers: UsrUserVO[];
}

export interface IssueStateVO {
    id: number;
    code: string;
    name: string;
    startState: boolean;
    finalState: boolean;
}

export interface IssueVO {
    id: number;
    issueListId: number;
    nodeId: number;
    number: number;
    issueTypeId: number;
    issueStateId: number;
    description: string;
    userCreate: UsrUserVO;
    timeCreated: string;
    referenceMark: string[];
}

export interface CommentVO {
    id: number;
    issueId: number;
    comment: string;
    user: UsrUserVO;
    prevStateId: number;
    nextStateId: number;
    timeCreated: string;
}

export interface UsrUserVO {
    username: string;
    id: string;
    active: boolean;
    description: string;
    party: Object;
    permissions: Object[];
    groups: Object[];
}

export interface CodelistData {
    aeTypes: ApTypeVO[];
    aeTypesMap: Record<number, ApTypeVO>;
    aeTypesTree: Array<ApTypeTreeVO>;
    dataTypes: RulDataTypeVO[];
    dataTypesMap: Record<number, RulDataTypeVO>;
    itemTypes: RulDescItemTypeExtVO[];
    itemTypesMap: Record<number, RulDescItemTypeExtVO>;
    partTypes: RulPartTypeVO[];
    partItemTypeInfoMap: Record<string, Record<number, RulPartTypeVO>>;
}

export interface ApTypeTreeVO extends ApTypeVO {
    children: Array<ApTypeTreeVO>;
    parent?: ApTypeTreeVO;
}

export type FundScope = { id: number, code: string, name: string, language: null | string };

export interface IFundFormData {
    name: string
    internalCode: string
    institutionId: string
    ruleSetId: string
    dateRange: string
    apScopes: FundScope[]
    fundAdmins: string[]
}

export interface DetailStoreState<T> {
    data?: T;
    id?: any;
    isFetching: boolean;
    fetched: boolean;
    currentDataKey: string;
    filter: Object;
}

export interface SimpleListStoreState<T> {
    rows?: T[];
    sourceRows?: T[];
    filteredRows?: T[];
    count: number;
    parent?: any;
    isFetching: boolean;
    fetched: boolean;
    currentDataKey: string;
    filter: Object;
}
