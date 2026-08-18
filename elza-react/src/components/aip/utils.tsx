import {i18n, Icon} from 'components/shared';
import { pad2 } from 'components/validate';
import {AipFieldName, LinkedNodeVO, LinkType, QueueItemState} from 'elza-api';
import { defineMessages, MessageDescriptor } from "react-intl";
import { AipValueType } from './filter/aipFilterModel';
import {Aips} from 'typings/store';
import {Button} from "react-bootstrap";
import {serverContextPath} from "../../api";

export const getAipRows = (aips: Aips) => {
    if(aips.fetched && aips.rows){
        if(
            aips.filter?.from &&
            aips.filter?.pageSize &&
            aips.filter.from > aips.filter.pageSize - 1
        ){
            return aips.rows;
        }
        return [
            ...aips.rows,
        ];
    }

    return [];
};

export const getBoolIcon = (bool: Boolean) => {
    return bool ? <Icon glyph="fa-check"/> : <Icon glyph="fa-close"/>;
}

export const getConnectedToJPIcon = (link: LinkType | null) => {
    let iconString = "fa fa-close";
    if(link == LinkType.Aip) {
        iconString="fa fa-check"
    }else if(link == LinkType.ComponentAip) {
        iconString="fa fa-chain-broken"
    } else if(link == LinkType.PartAip) {
        iconString="fa fa-link"
    }
    return <Icon glyph={iconString}/>;
}

export const getConnectedToJP = (linkedNodes: Array<LinkedNodeVO> | null, fundId: number, handleDeleteLink: any) => {
    let iconString = "fa fa-close";
    let nodes;

    if (linkedNodes) {
        iconString = "fa fa-check";

        nodes = linkedNodes.map(item =>
            <div key={item.id}>
                <a href={`${serverContextPath}/fund/${fundId}/node/${item.nodeId}`}>{item.name}</a>
                <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.id)}>
                    <Icon glyph="fa fa-close" />
                </Button>
            </div>)
    }

    return <div><Icon glyph={iconString}/> {nodes}</div>;
}

export const generateUUID = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0;
        const v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/**
 * Přeformátování datumu do DD.MM.YYYY řetězce.
 *
 * @param date
 * @returns {string} formatted date
 */
export const formatDate = (date: Date): string => {
    let month = date.getMonth() + 1,
        day = date.getDate(),
        year = date.getFullYear();
    return [pad2(day), pad2(month), year].join('.');
}

export const formatAipSize = (bytes: number): string => {
    if (bytes === 0) return '0 B';

    const k = 1024;
    const sizes = ['B', 'kB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    const size = (bytes / Math.pow(k, i)).toFixed(1);

    return `${size} ${sizes[i]}`;
}

export const messages = defineMessages({
    aipId:            { id: "aip.col.aipId",            defaultMessage: "ID Aipu" },
    code:             { id: "aip.col.code",             defaultMessage: "Kód aipu" },
    aipVersion:       { id: "aip.col.aipVersion",       defaultMessage: "Verze Aipu" },
    fund:             { id: "aip.col.fund",             defaultMessage: "Archivní soubor" },
    fundCode:         { id: "aip.col.fundCode",         defaultMessage: "Kód archivního souboru" },
    institution:      { id: "aip.col.institution",      defaultMessage: "Instituce" },
    institutionCode:  { id: "aip.col.institutionCode",  defaultMessage: "Kód instituce" },
    unitdate:         { id: "aip.col.unitdate",         defaultMessage: "Datace od-do" },
    originator:       { id: "aip.col.originator",       defaultMessage: "Původce" },
    ingestionCode:    { id: "aip.col.ingestionCode",    defaultMessage: "Číslo příjemky" },
    referenceNumber:  { id: "aip.col.referenceNumber",  defaultMessage: "Číslo jednací" },
    nadChangeCode:    { id: "aip.col.nadChangeCode",    defaultMessage: "Vnější změna" },
    aipSize:          { id: "aip.col.aipSize",          defaultMessage: "Velikost" },
    metadataLoad:     { id: "aip.col.metadataLoad",     defaultMessage: "Načtená metadata" },
    importState:      { id: "aip.col.importState",      defaultMessage: "Aktuální verze" },
    exportState:      { id: "aip.col.exportState",      defaultMessage: "Stav exportu" },
    completeAipLoad:  { id: "aip.col.completeAipLoad",  defaultMessage: "Načtený kompletní AIP" },
    metadataError:    { id: "aip.col.metadataError",    defaultMessage: "Chyba při načtení metadat" },
});

export const queueStateMessages = defineMessages({
    [QueueItemState.ImportError]: { id: "aip.queueState.IMPORT_ERROR", defaultMessage: "Chyba stažení" },
    [QueueItemState.ImportNew]:   { id: "aip.queueState.IMPORT_NEW",   defaultMessage: "Ke stažení" },
    [QueueItemState.ImportOk]:    { id: "aip.queueState.IMPORT_OK",    defaultMessage: "Aktualizováno/Staženo" },
    [QueueItemState.Update]:      { id: "aip.queueState.UPDATE",       defaultMessage: "K aktualizaci" },
    [QueueItemState.ExportError]: { id: "aip.queueState.EXPORT_ERROR", defaultMessage: "Chyba exportu" },
    [QueueItemState.ExportNew]:   { id: "aip.queueState.EXPORT_NEW",   defaultMessage: "K exportu" },
    [QueueItemState.ExportOk]:    { id: "aip.queueState.EXPORT_OK",    defaultMessage: "Exportováno" },
});

export const boolMessages = defineMessages({
    yes: { id: "aip.value.yes", defaultMessage: "ANO" },
    no:  { id: "aip.value.no",  defaultMessage: "NE" },
});

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
    [AipFieldName.MetadataError]: {key: "metadataError", message: messages.metadataError, valueType: "bool", minWidth: 70, idealWidth: 130},
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
    AipFieldName.Institution,
    AipFieldName.InstitutionCode,
    AipFieldName.Unitdate,
    AipFieldName.Originator,
    AipFieldName.IngestionCode,
    AipFieldName.ReferenceNumber,
    AipFieldName.NadChangeCode,
    AipFieldName.AipSize,
    AipFieldName.MetadataLoad,
    // TODO: @kasparova Bude upřesněno v budoucnu
    // Stažené komponenty, Napojen archivní popis
    AipFieldName.ImportState,
    AipFieldName.ExportState,
    AipFieldName.CompleteAipLoad,
    AipFieldName.MetadataError,
];

export type AipColumn = AipColumnDef & {field: AipFieldName};

export const colDef: AipColumn[] = COLUMN_ORDER.map(field => ({field, ...aipColumns[field]}));

export const findColDefByKey = (key: string) => {
    return colDef.find(item => item.key == key);
}

export const findColDefByField = (field: AipFieldName): AipColumn => {
    return {field, ...aipColumns[field]};
}
