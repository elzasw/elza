import { RulDescItemTypeExtVO } from '../../api/RulDescItemTypeExtVO';
import { ApTypeVO } from '../../api/ApTypeVO';
import { BaseRefTableStore } from '../BaseRefTableStore';
import { RulPartTypeVO } from '../../api/RulPartTypeVO';
import { RulDataTypeVO } from 'api/RulDataTypeVO';

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

export interface DescItemTypeRef extends RulDescItemTypeExtVO {
    dataType: RulDataTypeVO;
}

export interface OutputType {
    code?: string;
    id?: number;
    name?: string;
}

export interface Template {
    id?: number;
    code?: string;
    directory?: string;
    engine?: string;
    name?: string;
}

interface Templates {
    items?: Record<string, BaseRefTableStore<Template>>;
}

interface OutputFilterData {
    id?: number;
    name?: string;
    filename?: string;
    code?: string;
    packageId?: number;
    ruleSetId?: number;
}

interface OutputFilters {
    data: OutputFilterData[];
    currentDataKey?: boolean;
    id?: boolean;
    fetched?: boolean;
    isFetching?: boolean;
    getDataKey?: () => unknown;
    reducer?: () => unknown;
}

export interface RefTablesState {
    apTypes: BaseRefTableStore<ApTypeVO>;
    descItemTypes: BaseRefTableStore<DescItemTypeRef>;
    eidTypes: unknown;
    externalSystems: unknown;
    groups: unknown;
    institutions: unknown;
    issueStates: unknown;
    issueTypes: unknown;
    outputTypes: BaseRefTableStore<OutputType>;
    outputFilters: OutputFilters;
    partTypes: BaseRefTableStore<RulPartTypeVO>;
    recordTypes: BaseRefTableStore<unknown>;
    rulDataTypes: BaseRefTableStore<RulDataTypeVO>;
    ruleSet: unknown;
    scopesData: ScopesData;
    structureTypes: StructureTypes;
    templates: Templates;
    visiblePolicyTypes: BaseRefTableStore<VisiblePolicyRefItem>;
}
