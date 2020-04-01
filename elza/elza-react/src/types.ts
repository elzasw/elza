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
