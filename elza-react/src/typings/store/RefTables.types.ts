import { RulDescItemTypeVO } from '../../api/RulDescItemTypeVO';
import { ApTypeVO } from '../../api/ApTypeVO';
import { BaseRefTableStore } from '../BaseRefTableStore';

export interface CalendarType {
    code: string;
    id: number;
    name: string;
}

export interface Scope {
    versionId: number;
    id?: number | null;
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

export interface PartType {
    childPartId: number | null;
    code: PartTypeCodes;
    id: number;
    name: string;
    repeatable: boolean;
}

export interface RefTablesState {
    apTypes: BaseRefTableStore<ApTypeVO>;
    calendarTypes: BaseRefTableStore<CalendarType>;
    descItemTypes: BaseRefTableStore<RulDescItemTypeVO>;
    eidTypes: unknown;
    externalSystems: unknown;
    groups: unknown;
    institutions: unknown;
    issueStates: unknown;
    issueTypes: unknown;
    outputTypes: unknown;
    partTypes: BaseRefTableStore<PartType>;
    recordTypes: unknown;
    rulDataTypes: BaseRefTableStore<unknown>;
    ruleSet: unknown;
    scopesData: ScopesData;
    structureTypes: unknown;
    templates: unknown;
    visiblePolicyTypes: unknown;
}
