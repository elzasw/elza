import { defineMessages } from "react-intl";

/**
 * Popisky detailu archivni entity - hlavicka, casti, sekce a navazane
 * externi systemy.
 * 
 * Generovano primo z legacy katalogu, aby pri prepisu nevznikl preklep.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const apDetailMessages = defineMessages({
    bindingActionSynchronize: { id: "ap.binding.action.synchronize", defaultMessage: "Aktualizovat údaje" },
    bindingActionSynchronizeConfirmation: { id: "ap.binding.action.synchronize.confirmation", defaultMessage: "Opravdu chcete aktualizovat záznam entity z externího systému? Případné lokální změny těch částí záznamu entity, které existují v externím systému, mohou být přepsány." },
    bindingActionTakeRelEntities: { id: "ap.binding.action.take-rel-entities", defaultMessage: "Převzetí napojených entit" },
    bindingActionUpdate: { id: "ap.binding.action.update", defaultMessage: "Zapsat změny" },
    bindingExtStateAPPROVED: { id: "ap.binding.extState.APPROVED", defaultMessage: "schválená" },
    bindingExtStateERS_APPROVED: { id: "ap.binding.extState.ERS_APPROVED", defaultMessage: "schválená" },
    bindingExtStateERS_INVALID: { id: "ap.binding.extState.ERS_INVALID", defaultMessage: "neplatná" },
    bindingExtStateERS_NEW: { id: "ap.binding.extState.ERS_NEW", defaultMessage: "nová" },
    bindingExtStateERS_REPLACED: { id: "ap.binding.extState.ERS_REPLACED", defaultMessage: "nahrazená" },
    bindingIssuesNone: { id: "ap.binding.issues.none", defaultMessage: "Žádné problémy" },
    bindingProcessingSynchronize: { id: "ap.binding.processing.synchronize", defaultMessage: "Probíhá aktualizace údajů z ext. systému..." },
    bindingProcessingTakeRelEntities: { id: "ap.binding.processing.take-rel-entities", defaultMessage: "Probíhá založení přístupových bodů z návazných..." },
    bindingSource: { id: "ap.binding.source", defaultMessage: "zdroj" },
    bindingSyncStateLOCAL_CHANGE: { id: "ap.binding.syncState.LOCAL_CHANGE", defaultMessage: "Existuje lokální změna" },
    bindingSyncStateNOT_SYNCED: { id: "ap.binding.syncState.NOT_SYNCED", defaultMessage: "Nesynchronizuje se" },
    bindingSyncStateSYNC_OK: { id: "ap.binding.syncState.SYNC_OK", defaultMessage: "Aktivní synchronizace" },
    bindingUser: { id: "ap.binding.user", defaultMessage: "upravil" },
    coordinateExportInfo: { id: "ap.coordinate.export.info", defaultMessage: "Zvolte požadovaný formát exportovaných souřadnic:" },
    coordinateFormat: { id: "ap.coordinate.format", defaultMessage: "Formát {0}" },
    coordinateImportSelect: { id: "ap.coordinate.import.select", defaultMessage: "Vyberte soubor" },
    detailHistoryTitle: { id: "ap.detail.history.title", defaultMessage: "Historie stavů ({count})" },
    detailHistoryHide: { id: "ap.detail.history.hide", defaultMessage: "Skrýt panel" },
    detailPartPreferredNew: { id: "ap.detail.part.preferred.new", defaultMessage: "Nové preferované" },
    detailPartPreferredOld: { id: "ap.detail.part.preferred.old", defaultMessage: "Předchozí preferované" },
    detailPartPreferred: { id: "ap.detail.part.preferred", defaultMessage: "Preferované" },
    detailPartNoItems: { id: "ap.detail.part.noItems", defaultMessage: "Nejsou definovány žádné hodnoty atributů" },
    detailCollapseAll: { id: "ap.detail.collapseAll", defaultMessage: "Sbalit všechny části" },
    detailExpandAll: { id: "ap.detail.expandAll", defaultMessage: "Rozbalit všechny části" },
    detailShowDetails: { id: "ap.detail.showDetails", defaultMessage: "Zobrazit podrobnosti" },
    detailHideDetails: { id: "ap.detail.hideDetails", defaultMessage: "Skrýt podrobnosti" },
    revisionEmptyValue: { id: "ap.revision.emptyValue", defaultMessage: "Nevyplněno" },
    revisionDeletedValue: { id: "ap.revision.deletedValue", defaultMessage: "Smazáno" },
    detailAdd: { id: "ap.detail.add", defaultMessage: "Přidat {0}" },
    detailAddRelated: { id: "ap.detail.add.related", defaultMessage: "Přidat vztah" },
    detailCollapseInfo: { id: "ap.detail.collapseInfo", defaultMessage: "skrýt podrobnosti" },
    detailDelete: { id: "ap.detail.delete", defaultMessage: "Smazat" },
    detailEdit: { id: "ap.detail.edit", defaultMessage: "Upravit {0}" },
    detailExpandInfo: { id: "ap.detail.expandInfo", defaultMessage: "zobrazit podrobnosti" },
    detailInfo: { id: "ap.detail.info", defaultMessage: "popis záznamu entity" },
    detailLastChange: { id: "ap.detail.lastChange", defaultMessage: "Poslední změna" },
    detailLastChangeNotAvailable: { id: "ap.detail.lastChange.notAvailable", defaultMessage: "Informace o poslední změně není dostupná" },
    detailLastChangeUserNotAvailable: { id: "ap.detail.lastChange.user.notAvailable", defaultMessage: "Uživatel neuveden" },
    detailModifiedBy: { id: "ap.detail.modifiedBy", defaultMessage: "Upravil" },
    detailNoInfo: { id: "ap.detail.noInfo", defaultMessage: "Sekce neobsahuje žádné informace" },
    detailReplacedBy: { id: "ap.detail.replacedBy", defaultMessage: "Nahrazující entita" },
    detailReplacedEntityNoName: { id: "ap.detail.replacedEntity.noName", defaultMessage: "Bez názvu" },
    detailReplacingEntities: { id: "ap.detail.replacingEntities", defaultMessage: "Nahrazuje entity" },
    detailRevert: { id: "ap.detail.revert", defaultMessage: "Vrátit zpět" },
    detailSetPreferred: { id: "ap.detail.setPreferred", defaultMessage: "Označit jako preferované" },
    extSearchAssignedTo: { id: "ap.ext-search.assignedTo", defaultMessage: "Přiděleno" },
    formRefValue: { id: "ap.form.ref.value", defaultMessage: "Externí entita [{0}]" },
    notInExt: { id: "ap.not-in-ext", defaultMessage: "Nesdíleno" },
    pushToExt: { id: "ap.push-to-ext", defaultMessage: "Zápis entity do externího systému" },
    pushToExtTitle: { id: "ap.push-to-ext.title", defaultMessage: "Zápis entity" },
    arrNodeStatusErrErrors: { id: "arr.node.status.err.errors", defaultMessage: "Chyby" },
    arrNodeStatusOk: { id: "arr.node.status.ok", defaultMessage: "Ok" },
    globalActionExport: { id: "global.action.export", defaultMessage: "Exportovat" },
    globalValidationLoading: { id: "global.validation.loading", defaultMessage: "Načítání výsledku validace" },
    globalValidationRun: { id: "global.validation.run", defaultMessage: "Spustit validaci" },
    registryScopeClass: { id: "registry.scopeClass", defaultMessage: "Oblast" },
    registryVersion: { id: "registry.version", defaultMessage: "Verze" },
});

/** Stav synchronizace vazby; klíč se dřív skládal z hodnoty enumu SyncState. */
export const syncStateMessages = {
    SYNC_OK: apDetailMessages.bindingSyncStateSYNC_OK,
    NOT_SYNCED: apDetailMessages.bindingSyncStateNOT_SYNCED,
    LOCAL_CHANGE: apDetailMessages.bindingSyncStateLOCAL_CHANGE,
};

/** Stav entity v externím systému; skládal se z hodnoty vrácené serverem. */
export const extStateMessages = {
    APPROVED: apDetailMessages.bindingExtStateAPPROVED,
    ERS_APPROVED: apDetailMessages.bindingExtStateERS_APPROVED,
    ERS_INVALID: apDetailMessages.bindingExtStateERS_INVALID,
    ERS_NEW: apDetailMessages.bindingExtStateERS_NEW,
    ERS_REPLACED: apDetailMessages.bindingExtStateERS_REPLACED,
};
