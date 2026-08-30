import { AipFieldName } from "elza-api";
import { MessageDescriptor } from "react-intl";
import { AipValueType } from "./filter/aipFilterModel";
import { messages } from "./messages";

export type AipColumnDef = {
    /** Column identity in the table, and the property of AipDetailVO it renders. */
    key: string;
    message: MessageDescriptor;
    valueType: AipValueType;
    minWidth: number;
    idealWidth: number;
};

/**
 * Presentation of every field the filter contract offers.
 *
 * Keyed by AipFieldName, so a field added to the contract fails to compile here until it is
 * given a label and a value type. Where a field is filtered by is decided by the server;
 * this only says how it looks and which kind of value it holds.
 */
export const aipColumns: Record<AipFieldName, AipColumnDef> = {
    [AipFieldName.AipId]: {key: "aipId", message: messages.aipId, valueType: "number", minWidth: 50, idealWidth: 70},
    [AipFieldName.Code]: {key: "code", message: messages.code, valueType: "text", minWidth: 50, idealWidth: 70},
    [AipFieldName.AipVersion]: {key: "aipVersion", message: messages.aipVersion, valueType: "text", minWidth: 60, idealWidth: 90},
    [AipFieldName.Fund]: {key: "fund.name", message: messages.fund, valueType: "ref", minWidth: 70, idealWidth: 115},
    [AipFieldName.FundCode]: {key: "fundCode", message: messages.fundCode, valueType: "text", minWidth: 70, idealWidth: 115},
    [AipFieldName.Institution]: {key: "institution.name", message: messages.institution, valueType: "ref", minWidth: 70, idealWidth: 70},
    [AipFieldName.InstitutionCode]: {key: "institutionCode", message: messages.institutionCode, valueType: "text", minWidth: 70, idealWidth: 70},
    [AipFieldName.Unitdate]: {key: "unitdateFrom", message: messages.unitdate, valueType: "date", minWidth: 60, idealWidth: 105},
    [AipFieldName.Originator]: {key: "originator", message: messages.originator, valueType: "ref", minWidth: 70, idealWidth: 70},
    [AipFieldName.IngestionCode]: {key: "ingestionCode", message: messages.ingestionCode, valueType: "text", minWidth: 65, idealWidth: 105},
    [AipFieldName.ReferenceNumber]: {key: "referenceNumber", message: messages.referenceNumber, valueType: "text", minWidth: 60, idealWidth: 100},
    [AipFieldName.NadChangeCode]: {key: "nadChangeCode", message: messages.nadChangeCode, valueType: "text", minWidth: 60, idealWidth: 100},
    [AipFieldName.AipSize]: {key: "aipSize", message: messages.aipSize, valueType: "number", minWidth: 65, idealWidth: 65},
    [AipFieldName.MetadataLoad]: {key: "metadataLoad", message: messages.metadataLoad, valueType: "bool", minWidth: 70, idealWidth: 130},
    [AipFieldName.ImportState]: {key: "importState", message: messages.importState, valueType: "importState", minWidth: 65, idealWidth: 105},
    [AipFieldName.ExportState]: {key: "exportState", message: messages.exportState, valueType: "exportState", minWidth: 65, idealWidth: 105},
    [AipFieldName.CompleteAipLoad]: {key: "completeAipLoad", message: messages.completeAipLoad, valueType: "bool", minWidth: 70, idealWidth: 130},
    [AipFieldName.ProblemType]: {key: "problemType", message: messages.problemType, valueType: "problemType", minWidth: 70, idealWidth: 130},
    [AipFieldName.LinkState]: {key: "linkState", message: messages.linkState, valueType: "linkState", minWidth: 70, idealWidth: 130},
};

/**
 * Order the columns appear in.
 */
const COLUMN_ORDER: AipFieldName[] = [
    AipFieldName.AipId,
    AipFieldName.Code,
    AipFieldName.AipVersion,
    AipFieldName.Fund,
    AipFieldName.FundCode,
    AipFieldName.InstitutionCode,
    AipFieldName.Unitdate,
    AipFieldName.Originator,
    AipFieldName.IngestionCode,
    AipFieldName.ReferenceNumber,
    AipFieldName.NadChangeCode,
    AipFieldName.AipSize,
    AipFieldName.MetadataLoad,
    AipFieldName.LinkState,
    // TODO: @kasparova Bude upřesněno v budoucnu
    // Stažené komponenty
    AipFieldName.ImportState,
    AipFieldName.ExportState,
    AipFieldName.CompleteAipLoad,
    AipFieldName.ProblemType,
];

export type AipColumn = AipColumnDef & {field: AipFieldName};

export const colDef: AipColumn[] = COLUMN_ORDER.map(field => ({field, ...aipColumns[field]}));

export const findColDefByKey = (key: string) => {
    return colDef.find(item => item.key == key);
}

export const findColDefByField = (field: AipFieldName): AipColumn => {
    return {field, ...aipColumns[field]};
}
