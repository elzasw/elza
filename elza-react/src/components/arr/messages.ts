import { defineMessages } from "react-intl";

/**
 * Popisky obrazovek poradani.
 * 
 * Sdili je filtr fondu, historie zmen a hromadne upravy; generovano primo
 * z legacy katalogu, aby pri prepisu desitek hlasek nevznikl preklep.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const arrMessages = defineMessages({
    fundBulkModificationsActionFindAndReplace: { id: "arr.fund.bulkModifications.action.findAndReplace", defaultMessage: "Najít a nahradit" },
    fundBulkModificationsActionReplace: { id: "arr.fund.bulkModifications.action.replace", defaultMessage: "Nastavit" },
    fundBulkModificationsActionDelete: { id: "arr.fund.bulkModifications.action.delete", defaultMessage: "Odstranit" },
    fundBulkModificationsActionSetSpecification: { id: "arr.fund.bulkModifications.action.setSpecification", defaultMessage: "Nastavit" },
    fundBulkModificationsReplaceReplaceSpec: { id: "arr.fund.bulkModifications.replace.replaceSpec", defaultMessage: "Nová specifikace" },
    fundBulkModificationsOperationTypeSetSpecification: { id: "arr.fund.bulkModifications.operationType.setSpecification", defaultMessage: "Nahradit specifikaci za jinou" },
    fundBulkModificationsValues: { id: "arr.fund.bulkModifications.values", defaultMessage: "Aplikovat na hodnoty" },
    fundBulkModificationsSpecs: { id: "arr.fund.bulkModifications.specs", defaultMessage: "Aplikovat na specifikace" },
    historyDeleteQuestion: { id: "arr.history.deleteQuestion", defaultMessage: "Opravdu chcete provést nenávratné odstranění změn? (počet změn, které budou odstraněny: {0})" },
    historyTitleChangesForDelete: { id: "arr.history.title.changesForDelete", defaultMessage: "Změn k odstranění: {0}" },
    fundBulkModificationsFindAndRFeplaceFindText: { id: "arr.fund.bulkModifications.findAndRFeplace.findText", defaultMessage: "Najít" },
    fundBulkModificationsFindAndRFeplaceReplaceText: { id: "arr.fund.bulkModifications.findAndRFeplace.replaceText", defaultMessage: "Nahradit" },
    fundBulkModificationsItemsAreaAll: { id: "arr.fund.bulkModifications.itemsArea.all", defaultMessage: "Všechny v archivním souboru" },
    fundBulkModificationsItemsAreaPage: { id: "arr.fund.bulkModifications.itemsArea.page", defaultMessage: "Všechny na stránce" },
    fundBulkModificationsItemsAreaSelected: { id: "arr.fund.bulkModifications.itemsArea.selected", defaultMessage: "Zaškrtnuté na stránce" },
    fundBulkModificationsItemsAreaUnselected: { id: "arr.fund.bulkModifications.itemsArea.unselected", defaultMessage: "Odškrtnuté na stránce" },
    fundBulkModificationsOperationTypeAppend: { id: "arr.fund.bulkModifications.operationType.append", defaultMessage: "Přidat obsah prvku popisu" },
    fundBulkModificationsOperationTypeDelete: { id: "arr.fund.bulkModifications.operationType.delete", defaultMessage: "Odstranit celý prvek popisu" },
    fundBulkModificationsOperationTypeFindAndReplace: { id: "arr.fund.bulkModifications.operationType.findAndReplace", defaultMessage: "Najít a nahradit část obsahu" },
    fundBulkModificationsOperationTypeReplace: { id: "arr.fund.bulkModifications.operationType.replace", defaultMessage: "Nastavit celý obsah prvku" },
    fundBulkModificationsOperationTypeSetEnum: { id: "arr.fund.bulkModifications.operationType.setEnum", defaultMessage: "Nahradit hodnotu za jinou" },
    fundBulkModificationsReplaceReplaceEnum: { id: "arr.fund.bulkModifications.replace.replaceEnum", defaultMessage: "Nová hodnota" },
    fundBulkModificationsReplaceReplaceText: { id: "arr.fund.bulkModifications.replace.replaceText", defaultMessage: "Nová hodnota" },
    fundFilterSettingsActionClear: { id: "arr.fund.filterSettings.action.clear", defaultMessage: "Zrušit filtr" },
    fundFilterSettingsConditionBegin: { id: "arr.fund.filterSettings.condition.begin", defaultMessage: "Začíná na" },
    fundFilterSettingsConditionContainEntity: { id: "arr.fund.filterSettings.condition.containEntity", defaultMessage: "Obsahuje entitu" },
    fundFilterSettingsConditionCoordinatesNear: { id: "arr.fund.filterSettings.condition.coordinates.near", defaultMessage: "Je poblíž" },
    fundFilterSettingsConditionCoordinatesSubset: { id: "arr.fund.filterSettings.condition.coordinates.subset", defaultMessage: "Je součástí" },
    fundFilterSettingsConditionEmpty: { id: "arr.fund.filterSettings.condition.empty", defaultMessage: "Nevyplněn" },
    fundFilterSettingsConditionEnd: { id: "arr.fund.filterSettings.condition.end", defaultMessage: "Končí na" },
    fundFilterSettingsConditionEq: { id: "arr.fund.filterSettings.condition.eq", defaultMessage: "Je přesně" },
    fundFilterSettingsConditionGe: { id: "arr.fund.filterSettings.condition.ge", defaultMessage: "Větší než nebo rovno" },
    fundFilterSettingsConditionGt: { id: "arr.fund.filterSettings.condition.gt", defaultMessage: "Větší než" },
    fundFilterSettingsConditionInterval: { id: "arr.fund.filterSettings.condition.interval", defaultMessage: "Je mezi" },
    fundFilterSettingsConditionLe: { id: "arr.fund.filterSettings.condition.le", defaultMessage: "Menší než nebo rovno" },
    fundFilterSettingsConditionLt: { id: "arr.fund.filterSettings.condition.lt", defaultMessage: "Menší než" },
    fundFilterSettingsConditionNe: { id: "arr.fund.filterSettings.condition.ne", defaultMessage: "Různé od" },
    fundFilterSettingsConditionNone: { id: "arr.fund.filterSettings.condition.none", defaultMessage: "Bez podmínky" },
    fundFilterSettingsConditionNotEmpty: { id: "arr.fund.filterSettings.condition.notEmpty", defaultMessage: "Vyplněn" },
    fundFilterSettingsConditionNotInterval: { id: "arr.fund.filterSettings.condition.notInterval", defaultMessage: "Není mezi" },
    fundFilterSettingsConditionStringContain: { id: "arr.fund.filterSettings.condition.string.contain", defaultMessage: "Obsahuje" },
    fundFilterSettingsConditionStringNotContain: { id: "arr.fund.filterSettings.condition.string.notContain", defaultMessage: "Neobsahuje" },
    fundFilterSettingsConditionUndefined: { id: "arr.fund.filterSettings.condition.undefined", defaultMessage: "Hodnota 'výjimka' (nezjištěno, neexistuje)" },
    fundFilterSettingsConditionUnitdateGt: { id: "arr.fund.filterSettings.condition.unitdate.gt", defaultMessage: "Je po" },
    fundFilterSettingsConditionUnitdateIntersect: { id: "arr.fund.filterSettings.condition.unitdate.intersect", defaultMessage: "Spadá částečně do období" },
    fundFilterSettingsConditionUnitdateLt: { id: "arr.fund.filterSettings.condition.unitdate.lt", defaultMessage: "Je před" },
    fundFilterSettingsConditionUnitdateSubset: { id: "arr.fund.filterSettings.condition.unitdate.subset", defaultMessage: "Spadá do celého období" },
    fundFilterSettingsFilterByConditionTitle: { id: "arr.fund.filterSettings.filterByCondition.title", defaultMessage: "Filtrovat podle podmínky" },
    fundFilterSettingsFilterBySpecificationTitle: { id: "arr.fund.filterSettings.filterBySpecification.title", defaultMessage: "Filtrovat podle specifikace" },
    fundFilterSettingsFilterByValueTitle: { id: "arr.fund.filterSettings.filterByValue.title", defaultMessage: "Filtrovat podle hodnoty" },
    fundFilterSettingsValueEmpty: { id: "arr.fund.filterSettings.value.empty", defaultMessage: "(Prázdné)" },
    historyActionDeleteChanges: { id: "arr.history.action.deleteChanges", defaultMessage: "Odstranit změny" },
    historyActionDeleteFromShow: { id: "arr.history.action.deleteFrom.show", defaultMessage: "Přejít" },
    historyActionGoToDate: { id: "arr.history.action.goToDate", defaultMessage: "Zobrazit" },
    historyDeleteInProgress: { id: "arr.history.delete.inProgress", defaultMessage: "Probíhá odstranění změn..." },
    historyTitleChangeDate: { id: "arr.history.title.change.date", defaultMessage: "Datum" },
    historyTitleChangeDescription: { id: "arr.history.title.change.description", defaultMessage: "Změna" },
    historyTitleChangeTime: { id: "arr.history.title.change.time", defaultMessage: "Čas" },
    historyTitleChangeType: { id: "arr.history.title.change.type", defaultMessage: "Typ změny" },
    historyTitleChangeUser: { id: "arr.history.title.change.user", defaultMessage: "Uživatel" },
    historyTitleDeleteFrom: { id: "arr.history.title.deleteFrom", defaultMessage: "Záznam, od kterého (včetně) budou odstraněny změny" },
    historyTitleGlobalChanges: { id: "arr.history.title.globalChanges", defaultMessage: "Globální" },
    historyTitleGoToDate: { id: "arr.history.title.goToDate", defaultMessage: "Dohledat změnu podle data" },
    historyTitleNodeChanges: { id: "arr.history.title.nodeChanges", defaultMessage: "Jednotka popisu" },
    historyTitleSelectNode: { id: "arr.history.title.selectNode", defaultMessage: "Je potřeba vybrat konkrétní JP" },
    globalActionSelect: { id: "global.action.select", defaultMessage: "Vybrat" },
});

/**
 * Vzdálenosti pro filtr "v okolí"; klíč se dřív skládal z hodnoty v metrech.
 * Jména musí začínat písmenem, proto prefix `m`.
 */
export const coordinatesNearMessages = defineMessages({
    m100: { id: "arr.fund.filterSettings.condition.coordinates.near.100", defaultMessage: "100 m" },
    m500: { id: "arr.fund.filterSettings.condition.coordinates.near.500", defaultMessage: "0,5 km" },
    m1000: { id: "arr.fund.filterSettings.condition.coordinates.near.1000", defaultMessage: "1 km" },
    m10000: { id: "arr.fund.filterSettings.condition.coordinates.near.10000", defaultMessage: "10 km" },
    m20000: { id: "arr.fund.filterSettings.condition.coordinates.near.20000", defaultMessage: "20 km" },
    m50000: { id: "arr.fund.filterSettings.condition.coordinates.near.50000", defaultMessage: "50 km" },
    m100000: { id: "arr.fund.filterSettings.condition.coordinates.near.100000", defaultMessage: "100 km" },
});
