import {DataTypeVO, ItemSpecVO, ItemTypeInfoVO, ItemTypeVO, PartType} from "./api/generated/model";
import {ApTypeVO} from "./api/ApTypeVO";

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
    dataTypes: DataTypeVO[];
    dataTypesMap: Record<number, DataTypeVO>;
    itemSpecs: ItemSpecVO[];
    itemSpecsMap: Record<number, ItemSpecVO>;
    itemTypes: ItemTypeVO[];
    itemTypesMap: Record<number, ItemTypeVO>;
    partItemTypeInfoMap: Record<string, Record<number, ItemTypeInfoVO>>;
    partTypes: Array<PartType>;
}

export interface ApTypeTreeVO extends ApTypeVO {
    children: Array<ApTypeTreeVO>;
    parent?: ApTypeTreeVO;
}

export type FundScope = {id: number, code: string, name: string, language: null | string};

export interface IFundFormData {
    name: string
    internalCode: string
    institutionId: string
    ruleSetId: string
    dateRange: string
    apScopes: FundScope[]
    fundAdmins: string[]
}
