import { defineMessages } from "react-intl";

/**
 * Popisky editace jednotky popisu - panel JP, formular pridani JP,
 * lista nastroju, tabulkove zobrazeni a prvky popisu.
 * 
 * Generovano primo z legacy katalogu, aby pri prepisu nevznikl preklep.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const nodeMessages = defineMessages({
    accordionTitleLeftNameUndefined: { id: "accordion.title.left.name.undefined", defaultMessage: "'<'JP-Accordion left ID={0}'>'" },
    apCoordinateExportTitle: { id: "ap.coordinate.export.title", defaultMessage: "Export souřadnic" },
    apCoordinateImportTitle: { id: "ap.coordinate.import.title", defaultMessage: "Importovat souřadnice" },
    daosNodeSyncConfirmMessage: { id: "arr.daos.node.sync.confirm-message", defaultMessage: "Opravdu chcete synchronizovat digitální entity přiřazené k JP?" },
    daosNodeSyncSubmittingMessage: { id: "arr.daos.node.sync.submitting-message", defaultMessage: "Probíhá synchronizace DAO pro JP..." },
    daosNodeSyncTitle: { id: "arr.daos.node.sync.title", defaultMessage: "Synchronizovat všechny DAO přiřazené k JP" },
    fundAddNodeAfter: { id: "arr.fund.addNode.after", defaultMessage: "Za" },
    fundAddNodeAtEnd: { id: "arr.fund.addNode.atEnd", defaultMessage: "Na konec" },
    fundAddNodeBefore: { id: "arr.fund.addNode.before", defaultMessage: "Před" },
    fundAddNodeChild: { id: "arr.fund.addNode.child", defaultMessage: "Pod" },
    fundAddNodeCount: { id: "arr.fund.addNode.count", defaultMessage: "Počet" },
    fundAddNodeDirection: { id: "arr.fund.addNode.direction", defaultMessage: "Umístění" },
    fundAddNodeIgnoreRootNodes: { id: "arr.fund.addNode.ignoreRootNodes", defaultMessage: "Ignorovat kořenové uzly" },
    fundAddNodeNoDirection: { id: "arr.fund.addNode.noDirection", defaultMessage: "Nebylo vybráno umístění." },
    fundAddNodeRefTemplate: { id: "arr.fund.addNode.refTemplate", defaultMessage: "Synchronizační šablona" },
    fundAddNodeScenario: { id: "arr.fund.addNode.scenario", defaultMessage: "Typ jednotky popisu" },
    fundAddNodeTypeExisting: { id: "arr.fund.addNode.type.existing", defaultMessage: "Existující" },
    fundAddNodeTypeExistingArchiveFile: { id: "arr.fund.addNode.type.existing.archiveFile", defaultMessage: "Archivní soubor" },
    fundAddNodeTypeExistingFile: { id: "arr.fund.addNode.type.existing.file", defaultMessage: "Ze souboru" },
    fundAddNodeTypeExistingOther: { id: "arr.fund.addNode.type.existing.other", defaultMessage: "Z jiného AS...." },
    fundAddNodeTypeNew: { id: "arr.fund.addNode.type.new", defaultMessage: "Nová" },
    fundBulkModificationsAction: { id: "arr.fund.bulkModifications.action", defaultMessage: "Hromadná úprava prvku popisu" },
    fundBulkModificationsActionOpen: { id: "arr.fund.bulkModifications.action.open", defaultMessage: "Zobrazit ve formuláři JP" },
    fundBulkModificationsActionOpenInNewTab: { id: "arr.fund.bulkModifications.action.openInNewTab", defaultMessage: "Zobrazit v nové záložce formuláře JP" },
    fundBulkModificationsTitle: { id: "arr.fund.bulkModifications.title", defaultMessage: "Hromadná úprava prvku popisu" },
    fundBulkModificationsWarn: { id: "arr.fund.bulkModifications.warn", defaultMessage: "Opravdu chcete provést akci nad celým AS?" },
    fundColumnSettingsAction: { id: "arr.fund.columnSettings.action", defaultMessage: "Nastavit zobrazení sloupců" },
    fundColumnSettingsTitle: { id: "arr.fund.columnSettings.title", defaultMessage: "Nastavení sloupců" },
    fundDeleteNodeConfirm: { id: "arr.fund.deleteNode.confirm", defaultMessage: "Opravdu chcete smazat tuto jednotku popisu?" },
    fundFilterSettingsAction: { id: "arr.fund.filterSettings.action", defaultMessage: "Nastavit filtr pro prvek popisu" },
    fundFilterSettingsClearAllAction: { id: "arr.fund.filterSettings.clearAll.action", defaultMessage: "Odebrat všechny filtry" },
    fundFilterSettingsTitle: { id: "arr.fund.filterSettings.title", defaultMessage: "Nastavení filtru pro prvek popisu: {0}" },
    fundFilterSettingsUpdateDataAction: { id: "arr.fund.filterSettings.updateData.action", defaultMessage: "Aktualizovat data podle filtru" },
    fundJsonTableCellTitle: { id: "arr.fund.jsonTable.cell.title", defaultMessage: "Tabulka {0}x{1}" },
    fundNext: { id: "arr.fund.next", defaultMessage: "Následující" },
    fundPrev: { id: "arr.fund.prev", defaultMessage: "Předchozí" },
    fundRegScope: { id: "arr.fund.regScope", defaultMessage: "Oblast entit" },
    fundTitleReferendeMark: { id: "arr.fund.title.referendeMark", defaultMessage: "Číslo JP" },
    fundTitleSearch: { id: "arr.fund.title.search", defaultMessage: "Vyhledat v archivních souborech" },
    historyTitle: { id: "arr.history.title", defaultMessage: "Historie změn" },
    historyTitleNodeChanges: { id: "arr.history.title.nodeChanges", defaultMessage: "Jednotka popisu" },
    issuesAddNodeTitle: { id: "arr.issues.add.node.title", defaultMessage: "Přidat připomínku - Jednotka popisu" },
    nodeStatusErrErrors: { id: "arr.node.status.err.errors", defaultMessage: "Chyby" },
    nodeStatusErrMissing: { id: "arr.node.status.err.missing", defaultMessage: "Chybějící prvky" },
    nodeStatusOk: { id: "arr.node.status.ok", defaultMessage: "Ok" },
    nodeStatusOkx: { id: "arr.node.status.okx", defaultMessage: "Ok*" },
    nodeStatusUndefined: { id: "arr.node.status.undefined", defaultMessage: "Nezvalidovaný" },
    requestDigitizationRequestFormTitle: { id: "arr.request.digitizationRequest.form.title", defaultMessage: "Požadavek na digitalizát" },
    requestTitleTypeDAO_LINKLINK: { id: "arr.request.title.type.DAO_LINK.LINK", defaultMessage: "Připojení k" },
    requestTitleTypeDAO_LINKUNLINK: { id: "arr.request.title.type.DAO_LINK.UNLINK", defaultMessage: "Odpojení od" },
    syncNodesTitle: { id: "arr.syncNodes.title", defaultMessage: "Synchronizace JP ze zdrojových AS" },
    dataTypeCoordinatesFormat: { id: "dataType.coordinates.format", defaultMessage: "<div><b>Načtení souřadnic ze souboru</b> ve formátu<br />KML, GML nebo WKT<br /><b>Systém WGS84</b> (např. Mapy.cz)<br /><i>příklad: 49.5765442N, 14.3965617E</i><br /><b>Značkovací jazyk WKT</b><br /><i>příklady: POINT (14.3965617 49.5765442)</i><br /><i>LINESTRING (14.3965528 49.5765909,14.4172300 49.5551484)</i><br /><i>POLYGON ((14.3828494 49.5976066,14.3829031 49.5971094,<br />14.3842817 49.5971546,14.3842281 49.5976379,<br />14.3828494 49.5976066))</i></div>" },
    dataTypeUnitdateFormat: { id: "dataType.unitdate.format", defaultMessage: "<div><b>Formát datace</b><br />Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st<br />Rok: 1968<br />Měsíc: 8.1968<br />Den: 21.8.1968<br />Hodiny, minuty, sekundy: 21.8.1968 2:43 <i>nebo</i> 21.8.1968 8:23:31<br /><b>Intervaly</b><br />Roky: 1968-1969<br />Kombinace: 8.1968-1969 <i>nebo</i> 21.8.1968 2:43-27.6.1989<br /><b>Odhad</b><br />Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek:<br />Např.: [16.8.1977] <i>nebo</i> [1990]-1992<br />Při použití znaku \"/\" pro oddělení intervalu jsou od i do chápány jako odhad:<br />Např.: 1985/1990</div>" },
    globalActionCopyToClipboardFinished: { id: "global.action.copyToClipboard.finished", defaultMessage: "Zkopírováno do schránky" },
    globalActionSelect: { id: "global.action.select", defaultMessage: "Vybrat" },
    globalDataLoadingNode: { id: "global.data.loading.node", defaultMessage: "Načítání seznamu JP" },
    globalDataLoadingNodeChildren: { id: "global.data.loading.node.children", defaultMessage: "Načítání seznamu potomků JP" },
    globalGeometryLabelObjects: { id: "global.geometry.label.objects", defaultMessage: "Obrazce" },
    globalGeometryLabelPoints: { id: "global.geometry.label.points", defaultMessage: "Body" },
    globalTitleMoreRows: { id: "global.title.moreRows", defaultMessage: "Existují další záznamy ({0})." },
    subNodeFormAddFromTemplate: { id: "subNodeForm.add.fromTemplate", defaultMessage: "Ze šablony" },
    subNodeFormAddNoScenario: { id: "subNodeForm.add.noScenario", defaultMessage: "Bez scénáře" },
    subNodeFormCountOfCoordinates: { id: "subNodeForm.countOfCoordinates", defaultMessage: "Počet bodů: {0}" },
    subNodeFormDescItemCoordinatesActionAdd: { id: "subNodeForm.descItem.coordinates.action.add", defaultMessage: "Nahrát souřadnice" },
    subNodeFormDescItemLinkDescription: { id: "subNodeForm.descItem.link.description", defaultMessage: "Popisek" },
    subNodeFormDescItemLinkRefTemplate: { id: "subNodeForm.descItem.link.refTemplate", defaultMessage: "Šablona synchronizace" },
    subNodeFormDescItemLinkUri: { id: "subNodeForm.descItem.link.uri", defaultMessage: "URI" },
    subNodeFormDescItemTypeAll: { id: "subNodeForm.descItemType.all", defaultMessage: "Všechny prvky" },
    subNodeFormDescItemTypeCalculable: { id: "subNodeForm.descItemType.calculable", defaultMessage: "Hodnota počítána funkcí" },
    subNodeFormDescItemTypeUndefinedValue: { id: "subNodeForm.descItemType.undefinedValue", defaultMessage: "výjimka" },
    visiblePolicyActionSave: { id: "visiblePolicy.action.save", defaultMessage: "Upravit" },
    visiblePolicyExtensions: { id: "visiblePolicy.extensions", defaultMessage: "Rozšíření" },
    visiblePolicyFormTitle: { id: "visiblePolicy.form.title", defaultMessage: "Nastavení zobrazení pravidel kontroly JP" },
    visiblePolicyRules: { id: "visiblePolicy.rules", defaultMessage: "Pravidla kontroly" },
    visiblePolicyRulesNode: { id: "visiblePolicy.rules.node", defaultMessage: "Nastavení pro JP" },
    visiblePolicyRulesParent: { id: "visiblePolicy.rules.parent", defaultMessage: "Nastavení z vyšších úrovní" },
});

/** Smer vlozeni JP; klic se driv skladal z hodnoty mapy directions. */
export const addNodeDirectionMessages = {
    before: nodeMessages.fundAddNodeBefore,
    after: nodeMessages.fundAddNodeAfter,
    child: nodeMessages.fundAddNodeChild,
    atEnd: nodeMessages.fundAddNodeAtEnd,
};

/** Typ vazby DAO_LINK pozadavku; klic se skladal z hodnoty ze serveru. */
export const daoLinkTypeMessages = {
    LINK: nodeMessages.requestTitleTypeDAO_LINKLINK,
    UNLINK: nodeMessages.requestTitleTypeDAO_LINKUNLINK,
};

/**
 * Napoveda k formatu hodnoty, kterou ItemTooltipWrapper dostava jako prop.
 *
 * Hlasky jsou cele HTML bloky - v ICU je '<' znacka rich-textu, takze se
 * formatuji s { ignoreTag: true } a vkladaji pres dangerouslySetInnerHTML
 * (jde o staticky text ze zdrojaku, ne o uzivatelsky vstup).
 *
 * dataType.recordRef.format mel v legacy katalogu hodnotu null, takze se
 * tooltip nezobrazoval; mapa ho proto nema a chovani zustava stejne.
 */
export const formatHintMessages = {
    'dataType.coordinates.format': nodeMessages.dataTypeCoordinatesFormat,
    'dataType.unitdate.format': nodeMessages.dataTypeUnitdateFormat,
};
