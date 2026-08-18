import { defineMessages } from "react-intl";
import { QueueItemState } from "elza-api";

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
