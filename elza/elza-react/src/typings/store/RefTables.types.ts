import { RulDescItemTypeExtVO } from '../../api/RulDescItemTypeExtVO';
import { ApTypeVO } from '../../api/ApTypeVO';
import { BaseRefTableStore } from '../BaseRefTableStore';
import { RulPartTypeVO } from '../../api/RulPartTypeVO';

export interface Scope {
    versionId?: number;
    code?: string;
    id?: number | null;
    name: string;
    language: string | null;
    ruleSetCode?: string;
}

export interface ScopeData {
    isDirty: boolean | unknown;
    isFetching: boolean | unknown;
    versionId: number | unknown;
    scopes: Scope[];
}

export interface ScopesData {
    scopes: ScopeData[];
}

export enum PartTypeCodes {
    PT_NAME = "PT_NAME",
    PT_REL = "PT_REL",
    PT_IDENT = "PT_IDENT",
    PT_BODY = "PT_BODY",
    PT_CRE = "PT_CRE",
    PT_EXT = "PT_EXT",
    PT_EVENT = "PT_EVENT",
}

export interface VisiblePolicyRefItem {
    id: number;
    code: string;
    name: string;
    ruleSetId: number;
}

export interface StructureType {
    id: number;
    name: string;
    code: string;
    anonymous: boolean;
}

export interface StructureTypes {
    data?: [{
        data: StructureType[];
        isFetching: boolean;
        isDirty: boolean;
        versionId: number;
    }]
}

export interface RefTablesState {
    apTypes: BaseRefTableStore<ApTypeVO>;
    descItemTypes: BaseRefTableStore<RulDescItemTypeExtVO>;
    eidTypes: unknown;
    externalSystems: unknown;
    groups: unknown;
    institutions: unknown;
    issueStates: unknown;
    issueTypes: unknown;
    outputTypes: unknown;
    partTypes: BaseRefTableStore<RulPartTypeVO>;
    recordTypes: BaseRefTableStore<unknown>;
    rulDataTypes: BaseRefTableStore<unknown>;
    ruleSet: unknown;
    scopesData: ScopesData;
    structureTypes: StructureTypes;
    templates: unknown;
    visiblePolicyTypes: BaseRefTableStore<VisiblePolicyRefItem>;
}
