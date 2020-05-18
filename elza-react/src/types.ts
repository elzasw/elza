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

export type FundScope = {id: number, code: string, name: string, language: null | string};

export interface IFundFormData {
    name: string
    internalCode: string
    institutionIdentifier: string
    ruleSetId: string
    ruleSetCode: string
    dateRange: string
    scopes: string[]
    fundAdmins: string[]
    fundNumber?: number
    unitdate?: string
    mark?: string
}


export class UsrPermissionVO {
    id: number;
    inherited: boolean;
    groupId: number;
    permission: any;
    fund: any; // ArrFundBaseVO
    groupControl: UsrGroupVO;
    userControl: UsrUserVO;
    scope: any; // ApScopeVO
    node: any; // ArrNodeVO
    issueList: any; // WfIssueListBaseVO
}

export interface UsrGroupVO {
    id: number;
    code: string;
    name: string;
    description: string;
    permissions: UsrPermissionVO[];
    users: UsrUserVO[];
}

/**
 * @deprecated use {CreateFund}
 */
export interface CreateFundVO {
    name: string;
    ruleSetId: number;
    internalCode: string;
    institutionId: number;
    dateRange: string;
    adminUsers: UsrUserVO[];
    adminGroups: UsrGroupVO[];
}

export interface CreateFund {
    name: string;
    ruleSetCode: string;
    institutionIdentifier: string;
    internalCode: string;
    dateRange: string;
    uuid: string;
    fundNumber: number;
    unitdate: string;
    mark: string;
    adminUsers: number[];
    adminGroups: number[];
    scopes: string[];
}

export interface UpdateFund {
    name: string;
    ruleSetCode: string;
    institutionIdentifier: string;
    internalCode: string;
    dateRange: string;
    fundNumber: number;
    unitdate: string;
    mark: string;
    // adminUsers: number[];
    // adminGroups: number[];
    scopes: string[];
}

export interface Fund {
    id: number;
    uuid: string;
    name: string;
    institutionIdentifier: string;
    internalCode: string;
    createDate: string;
    fundNumber: number;
    unitdate: string;
}

export interface FindFundsResult {
    funds: Fund[];
    totalCount: number;
}
