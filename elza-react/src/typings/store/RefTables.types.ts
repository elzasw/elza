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

// interface OutputFilters {
//     data: OutputFilterData[];
//     currentDataKey?: boolean;
//     id?: boolean;
//     fetched?: boolean;
//     isFetching?: boolean;
//     getDataKey?: () => unknown;
//     reducer?: () => unknown;
// }

export interface RefTablesDataStore<T> {
    data: T[] | null;
    currentDataKey?: boolean;
    id?: boolean;
    fetched?: boolean;
    isFetching?: boolean;
    getDataKey?: () => unknown;
    reducer?: () => unknown;
}

export interface RefTablesDataMapStore<T> {
    data: Record<string, T> & { ids: string[] } | null;
    currentDataKey?: boolean;
    id?: boolean;
    fetched?: boolean;
    isFetching?: boolean;
    getDataKey?: () => unknown;
    reducer?: () => unknown;
}

enum IssueTypeEnum {
    IMPORTANT = "IMPORTANT",
    RECOMMENDED = "RECOMMENDED",
    MINOR = "MINOR",
}

export interface IssueType {
    id: number;
    code: IssueTypeEnum;
    name: string;
}

export interface InstitutionType {
    id: number;
    code: string;
    name: string;
}

export interface Institution {
    id: number;
    intitutionType: InstitutionType;
    accessPointId: number;
    name: string;
    code: string;
}

export enum RuleType {
    ARRANGEMENT = "ARRANGEMENT",
    ENTITY = "ENTITY",
}

export interface RuleSet {
    code: string;
    gridViews: unknown | null;
    id: number;
    name: string;
    ruleType: RuleType;
}

export interface DescItemGroup {
    code: string;
    itemTypes: Array<{ id: number, width: number }>;
    name: string;
}

/**
 * Simple external-system VO from the ref-tables list (backend
 * `SysExternalSystemSimpleVO` hierarchy; `@class` = MINIMAL_CLASS discriminator).
 * All subtypes carry `id`/`code`/`name`; only the AP one adds `scope`/`type`.
 */
export interface RefExternalSystemSimpleVOBase {
    /**
     * @deprecated LEGACY — the Jackson `@class` MINIMAL_CLASS discriminator leaked into
     * the client. Used for now to distinguish external-system subtypes; should be replaced
     * by a proper typed discriminator field (or generated API types) and removed.
     */
    '@class': string;
    id?: number;
    code?: string;
    name?: string;
}

export interface RefApExternalSystemSimpleVO extends RefExternalSystemSimpleVOBase {
    /** @deprecated LEGACY discriminator — see {@link RefExternalSystemSimpleVOBase}. */
    '@class': '.ApExternalSystemSimpleVO';
    scope?: number | null;
    type?: string;
}

export interface RefAiExternalSystemSimpleVO extends RefExternalSystemSimpleVOBase {
    /** @deprecated LEGACY discriminator — see {@link RefExternalSystemSimpleVOBase}. */
    '@class': '.AiExternalSystemSimpleVO';
}

export interface RefGisExternalSystemSimpleVO extends RefExternalSystemSimpleVOBase {
    /** @deprecated LEGACY discriminator — see {@link RefExternalSystemSimpleVOBase}. */
    '@class': '.GisExternalSystemSimpleVO';
}

export interface RefArrDigitalRepositorySimpleVO extends RefExternalSystemSimpleVOBase {
    /** @deprecated LEGACY discriminator — see {@link RefExternalSystemSimpleVOBase}. */
    '@class': '.ArrDigitalRepositorySimpleVO';
}

export interface RefArrDigitizationFrontdeskSimpleVO extends RefExternalSystemSimpleVOBase {
    /** @deprecated LEGACY discriminator — see {@link RefExternalSystemSimpleVOBase}. */
    '@class': '.ArrDigitizationFrontdeskSimpleVO';
}

export type RefExternalSystemSimpleVO =
    | RefApExternalSystemSimpleVO
    | RefAiExternalSystemSimpleVO
    | RefGisExternalSystemSimpleVO
    | RefArrDigitalRepositorySimpleVO
    | RefArrDigitizationFrontdeskSimpleVO;

export interface RefTablesState {
    apTypes: BaseRefTableStore<ApTypeVO>;
    descItemTypes: BaseRefTableStore<DescItemTypeRef>;
    eidTypes: unknown;
    externalSystems: BaseRefTableStore<RefExternalSystemSimpleVO>;
    groups: RefTablesDataMapStore<DescItemGroup>;
    institutions: BaseRefTableStore<Institution>;
    issueStates: unknown;
    issueTypes: RefTablesDataStore<IssueType>;
    outputTypes: BaseRefTableStore<OutputType>;
    outputFilters: RefTablesDataStore<OutputFilterData>;
    partTypes: BaseRefTableStore<RulPartTypeVO>;
    recordTypes: BaseRefTableStore<unknown>;
    rulDataTypes: BaseRefTableStore<RulDataTypeVO>;
    ruleSet: BaseRefTableStore<RuleSet>;
    scopesData: ScopesData;
    structureTypes: StructureTypes;
    templates: Templates;
    visiblePolicyTypes: BaseRefTableStore<VisiblePolicyRefItem>;
}
