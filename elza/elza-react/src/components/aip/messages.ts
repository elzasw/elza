import { defineMessages } from "react-intl";
import { AipLinkState, AipProblemType, AipUpdateType, DaAipActionItemState, QueueItemState } from "elza-api";

export const messages = defineMessages({
    aipId:            { id: "aip.col.aipId",            defaultMessage: "ID" },
    code:             { id: "aip.col.code",             defaultMessage: "AIP ID" },
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
    importState:      { id: "aip.col.importState",      defaultMessage: "Stav importu" },
    exportState:      { id: "aip.col.exportState",      defaultMessage: "Stav exportu" },
    completeAipLoad:  { id: "aip.col.completeAipLoad",  defaultMessage: "Načtený kompletní AIP" },
    problemType:      { id: "aip.col.problemType",      defaultMessage: "Problém" },
    linkState:        { id: "aip.col.linkState",        defaultMessage: "Napojení" },
});

/**
 * Typy aktualizace AIPu nabízené v dialogu aktualizace.
 */
export const updateTypeMessages = defineMessages({
    [AipUpdateType.DbUpdate]: {
        id: "aip.form.update.type.DbUpdate",
        defaultMessage: "Aktualizace pouze DB",
    },
    [AipUpdateType.DownloadUpdate]: {
        id: "aip.form.update.type.DownloadUpdate",
        defaultMessage: "Opakované stažení AIP",
    },
    [AipUpdateType.ForceUpdate]: {
        id: "aip.form.update.type.ForceUpdate",
        defaultMessage: "Vynucená aktualizace",
    },
    [AipUpdateType.RemapReferences]: {
        id: "aip.form.update.type.RemapReferences",
        defaultMessage: "Znovu dohledat instituci a fond",
    },
});

/**
 * Co která volba udělá, včetně dopadů - bez toho nelze volbu odpovědně vybrat.
 */
export const updateTypeDescriptions = defineMessages({
    [AipUpdateType.RemapReferences]: {
        id: "aip.form.update.type.RemapReferences.hint",
        defaultMessage: "Znovu dohledá instituci a fond podle kódů z balíčku. Použijte po založení "
            + "chybějící instituce nebo fondu. Je-li úložiště nastavené na automatické stahování "
            + "metadat, vyžádá se rovnou i jejich stažení.",
    },
    [AipUpdateType.DownloadUpdate]: {
        id: "aip.form.update.type.DownloadUpdate.hint",
        defaultMessage: "Vyžádá nové stažení balíčku z digitálního archivu. Stáhne se to, co je "
            + "u AIPu načtené - údaje balíčku, metadata, nebo úplný AIP.",
    },
    [AipUpdateType.DbUpdate]: {
        id: "aip.form.update.type.DbUpdate.hint",
        defaultMessage: "Znovu sestaví digitální entity v ELZA z už staženého balíčku, "
            + "z digitálního archivu se nestahuje nic. Skončí chybou, pokud by musela odstranit "
            + "entitu napojenou na jednotku popisu.",
    },
    [AipUpdateType.ForceUpdate]: {
        id: "aip.form.update.type.ForceUpdate.hint",
        defaultMessage: "Totéž jako aktualizace z uloženého balíčku, ale odstraní i digitální "
            + "entity napojené na jednotky popisu. Tato napojení se ztratí.",
    },
});

/**
 * Samostatná stránka průzkumníka AIPu.
 */
export const explorerPageMessages = defineMessages({
    back:          { id: "aip.explorer.back",          defaultMessage: "Zpět na seznam" },
    open:          { id: "aip.explorer.open",          defaultMessage: "Otevřít průzkumník" },
    packageTab:    { id: "aip.explorer.tab.package",   defaultMessage: "Balíček" },
    structureTab:  { id: "aip.explorer.tab.structure", defaultMessage: "Struktura" },
});

/**
 * Prohlížeč staženého balíčku.
 */
export const packageMessages = defineMessages({
    loading:        { id: "aip.package.loading",        defaultMessage: "Načítání balíčku…" },
    notDownloaded:  { id: "aip.package.notDownloaded",  defaultMessage: "Pro tento AIP není stažený žádný balíček." },
    empty:          { id: "aip.package.empty",          defaultMessage: "Balíček neobsahuje žádné soubory." },
    selectFile:     { id: "aip.package.selectFile",     defaultMessage: "Vyberte soubor balíčku." },
    notPreviewable: { id: "aip.package.notPreviewable", defaultMessage: "Soubor nelze zobrazit jako text, lze jej stáhnout." },
    readFailed:     { id: "aip.package.readFailed",     defaultMessage: "Soubor se nepodařilo načíst." },
    download:       { id: "aip.package.download",       defaultMessage: "Stáhnout" },
    downloadAll:    { id: "aip.package.downloadAll",    defaultMessage: "Stáhnout celý balíček" },
    treeLabel:      { id: "aip.package.treeLabel",      defaultMessage: "Obsah balíčku" },
    problemHint:    { id: "aip.package.problemHint",    defaultMessage: "Soubor, kterého se problém týká:" },
});

/**
 * Proč danou volbu nelze u vybraných AIPů použít.
 */
export const updateTypeUnavailable = defineMessages({
    needsMetadata: {
        id: "aip.form.update.unavailable.needsMetadata",
        defaultMessage: "Nelze použít: žádný z vybraných AIPů nemá stažená metadata.",
    },
    needsUnresolved: {
        id: "aip.form.update.unavailable.needsUnresolved",
        defaultMessage: "Nelze použít: žádný z vybraných AIPů nemá nedohledanou instituci ani fond.",
    },
});

/**
 * Popisky akcí v detailu AIPu.
 */
export const detailMessages = defineMessages({
    downloadPackage: { id: "aip.detail.downloadPackage", defaultMessage: "Stáhnout balíček" },
});

/**
 * Popis problémů, které brání zpracování AIPu nebo jeho navázání na archivní popis.
 */
export const problemMessages = defineMessages({
    [AipProblemType.MetadataError]: {
        id: "aip.problem.METADATA_ERROR",
        defaultMessage: "Chyba při zpracování metadat",
    },
    [AipProblemType.UnknownFund]: {
        id: "aip.problem.UNKNOWN_FUND",
        defaultMessage: "Nenalezen fond",
    },
    [AipProblemType.UnknownInstitution]: {
        id: "aip.problem.UNKNOWN_INSTITUTION",
        defaultMessage: "Nenalezena instituce",
    },
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

/**
 * Jak velká část AIPu je napojená na archivní popis.
 */
export const linkStateMessages = defineMessages({
    [AipLinkState.NotLinked]:       { id: "aip.linkState.NOT_LINKED",       defaultMessage: "Nenapojeno" },
    [AipLinkState.PartiallyLinked]: { id: "aip.linkState.PARTIALLY_LINKED", defaultMessage: "Částečně napojeno" },
    [AipLinkState.FullyLinked]:     { id: "aip.linkState.FULLY_LINKED",     defaultMessage: "Napojeno" },
});

export const boolMessages = defineMessages({
    yes: { id: "aip.value.yes", defaultMessage: "ANO" },
    no:  { id: "aip.value.no",  defaultMessage: "NE" },
});

/**
 * Popisky filtru seznamu AIP - porovnání, meze rozsahu a chyby formuláře.
 */
export const filterMessages = defineMessages({
    content:      { id: "aip.form.content",      defaultMessage: "Obsah" },
    value:        { id: "aip.form.value",        defaultMessage: "Hodnota" },
    from:         { id: "aip.form.from",         defaultMessage: "Od" },
    to:           { id: "aip.form.to",           defaultMessage: "Do" },
    equals:       { id: "aip.form.equals",       defaultMessage: "Je přesné" },
    notEquals:    { id: "aip.form.notEquals",    defaultMessage: "Není přesné" },
    contain:      { id: "aip.form.contain",      defaultMessage: "Obsahuje" },
    notContain:   { id: "aip.form.notContain",   defaultMessage: "Neobsahuje" },
    between:      { id: "aip.form.between",      defaultMessage: "Je v rozmezí" },
    isNull:       { id: "aip.form.null",         defaultMessage: "Nenastavena hodnota" },
    notNull:      { id: "aip.form.notNull",      defaultMessage: "Nastavena hodnota" },
    valueNull:    { id: "aip.filter.value.null",    defaultMessage: "Hodnota nenastavena" },
    valueNotNull: { id: "aip.filter.value.notNull", defaultMessage: "Hodnota nastavena" },
    search:       { id: "aip.table.search",      defaultMessage: "Vyhledávání" },
});

/**
 * Chyby validace formuláře filtru.
 */
export const filterErrorMessages = defineMessages({
    value:       { id: "aip.form.error.value",       defaultMessage: "Hodnota musí být vyplněna" },
    nan:         { id: "aip.form.error.nan",         defaultMessage: "Musí být číslo" },
    positiveNum: { id: "aip.form.error.positiveNum", defaultMessage: "Musí být kladné číslo" },
    between:     { id: "aip.form.error.between",     defaultMessage: "Číslo od musí být menší než číslo do" },
    emptyDate:   { id: "aip.form.error.emptyDate",   defaultMessage: "Datum musí být zadán" },
});

/**
 * Průběh a výsledek akce nad AIPy.
 */
export const actionMessages = defineMessages({
    running:     { id: "aip.action.running",     defaultMessage: "Probíhá…" },
    finished:    { id: "aip.action.finished",    defaultMessage: "Dokončeno" },
    withErrors:  { id: "aip.action.withErrors",  defaultMessage: "Dokončeno s chybami" },
    close:       { id: "aip.action.close",       defaultMessage: "Zavřít" },
    aipColumn:   { id: "aip.action.col.aip",     defaultMessage: "AIP" },
    stateColumn: { id: "aip.action.col.state",   defaultMessage: "Výsledek" },
    summary: {
        id: "aip.action.summary",
        defaultMessage: "Hotovo {done} z {total}{errors, plural, =0 {} one { · # chyba} few { · # chyby} other { · # chyb}}",
    },
    toastFinished: {
        id: "aip.action.toast.finished",
        defaultMessage: "{count, plural, one {Akce dokončena u # AIPu} few {Akce dokončena u # AIPů} other {Akce dokončena u # AIPů}}",
    },
    toastErrors: {
        id: "aip.action.toast.errors",
        defaultMessage: "{count, plural, one {Akce skončila chybou u # AIPu} few {Akce skončila chybou u # AIPů} other {Akce skončila chybou u # AIPů}}",
    },
});

/**
 * Výsledek akce u jednoho AIPu.
 */
export const actionItemStateMessages = defineMessages({
    [DaAipActionItemState.Waiting]:  { id: "aip.action.item.WAITING",  defaultMessage: "Čeká" },
    [DaAipActionItemState.Running]:  { id: "aip.action.item.RUNNING",  defaultMessage: "Probíhá" },
    [DaAipActionItemState.Finished]: { id: "aip.action.item.FINISHED", defaultMessage: "Hotovo" },
    [DaAipActionItemState.Error]:    { id: "aip.action.item.ERROR",    defaultMessage: "Chyba" },
    [DaAipActionItemState.Skipped]:  { id: "aip.action.item.SKIPPED",  defaultMessage: "Přeskočeno" },
});

/**
 * Co brání napojení vybraných AIPů; zjišťuje se před potvrzením.
 */
export const connectCheckMessages = defineMessages({
    blocked: {
        id: "aip.connect.blocked",
        defaultMessage: "{count, plural, one {# AIP je již napojen} few {# AIPy jsou již napojené} other {# AIPů je již napojených}} a úložiště neumožňuje více vazeb. Napojení nelze provést.",
    },
    showDetail: { id: "aip.connect.showDetail", defaultMessage: "Zobrazit seznam" },
    hideDetail: { id: "aip.connect.hideDetail", defaultMessage: "Skrýt seznam" },
});
