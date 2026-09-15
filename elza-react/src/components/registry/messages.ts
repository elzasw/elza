import { defineMessages } from "react-intl";

/**
 * Popisky obrazovky archivnich entit - seznam, ribbon a detail.
 *
 * Sdili je `pages/registry/RegistryPage`, `RegistryList`, `EntityRibbon`
 * a `ApDetailPageWrapper`; akce nad entitou se objevuji ve vsech ctyrech.
 *
 * Id jsou prevzata z legacy katalogu beze zmeny.
 */
export const messages = defineMessages({
  invalidatedEntities: {
    id: "invalidated_entities",
    defaultMessage: "Zneplatněné entity"
  },
});

export const registryMessages = defineMessages({
    stateComment: { id: "ap.state.title.comment", defaultMessage: "Komentář" },
    removeDuplicity: { id: "accesspoint.removeDuplicity", defaultMessage: "Odstranit duplicitu" },
    removeDuplicityConfirmation: { id: "accesspoint.removeDuplicity.confirmation", defaultMessage: "Opravdu chcete odstranit archivní entitu s rozpracovanou revizí?" },
    removeDuplicityTitle: { id: "accesspoint.removeDuplicity.title", defaultMessage: "Odstranění duplicity" },
    scopeManagementTitle: { id: "accesspoint.scope.management.title", defaultMessage: "Správa oblastí archivních entit" },
    changeState: { id: "ap.changeState", defaultMessage: "Změnit vlastnosti entity" },
    connect: { id: "ap.connect", defaultMessage: "Propojení s externím systémem" },
    copyTitle: { id: "ap.copy.title", defaultMessage: "Kopie entity" },
    detailDeleteConfirm: { id: "ap.detail.delete.confirm", defaultMessage: "Smazat celou část?" },
    detailDeleteConfirmValue: { id: "ap.detail.delete.confirm.value", defaultMessage: "Smazat celou část? ({0})" },
    detailEntityMissing: { id: "ap.detail.entityMissing", defaultMessage: "Entita neexistuje" },
    detailRevertConfirm: { id: "ap.detail.revert.confirm", defaultMessage: "Přejete si vrátit zpět původní hodnoty?" },
    extFilterAssignedToCurrentUser: { id: "ap.ext-filter.assignedTo.currentUser", defaultMessage: "Moje úkoly" },
    extFilterTitle: { id: "ap.ext-filter.title", defaultMessage: "Rozšířený filtr" },
    extFilterUse: { id: "ap.ext-filter.use", defaultMessage: "Použít rozšířený filtr" },
    extFilterUsed: { id: "ap.ext-filter.used", defaultMessage: "Je použitý rozšířený filtr" },
    extSearchTitle: { id: "ap.ext-search.title", defaultMessage: "Import ze systému" },
    extSearchTitleConnect: { id: "ap.ext-search.title-connect", defaultMessage: "Propojení s externím systémem" },
    extSyncsTitle: { id: "ap.ext-syncs.title", defaultMessage: "Synchronizace s externími systémy" },
    historyTitle: { id: "ap.history.title", defaultMessage: "Historie stavů" },
    pushToExtConfirmation: { id: "ap.push-to-ext.confirmation", defaultMessage: "Opravdu chcete odeslat záznam entity s rozpracovanou revizí?" },
    pushToExtFailedIntro: { id: "ap.push-to-ext.failed.intro", defaultMessage: "Odeslání entity se nezdařilo z následujících důvodů:" },
    pushToExtFailedMessage: { id: "ap.push-to-ext.failed.message", defaultMessage: "Nepodařilo se entitu uložit do návazného systému. Podrobnosti naleznete v okně Fronta synchronizace (Synchronizace s externími systémy)" },
    pushToExtFailedTitle: { id: "ap.push-to-ext.failed.title", defaultMessage: "Chyba při odesílání entity" },
    pushToExtNeedConfirmConfirm: { id: "ap.push-to-ext.needConfirm.confirm", defaultMessage: "Odeslat entitu" },
    pushToExtNeedConfirmIntro: { id: "ap.push-to-ext.needConfirm.intro", defaultMessage: "Při odesílání entity byly zjištěny možné nesrovnalosti. Přejete si entitu odeslat přesto?" },
    pushToExtNeedConfirmTitle: { id: "ap.push-to-ext.needConfirm.title", defaultMessage: "Upozornění na možné nesrovnalosti" },
    pushToExtPendingMessage: { id: "ap.push-to-ext.pending.message", defaultMessage: "Čekání na odeslání entity ..." },
    pushToExtStartedMessage: { id: "ap.push-to-ext.started.message", defaultMessage: "Odesílání entity ..." },
    pushToExtTitle: { id: "ap.push-to-ext.title", defaultMessage: "Zápis entity" },
    stateTitleRevComment: { id: "ap.state.title.revComment", defaultMessage: "Komentář revize" },
    stateHistory: { id: "ap.stateHistory", defaultMessage: "Historie stavů" },
    titleRegistry: { id: "import.title.registry", defaultMessage: "Import rejstříkových hesel" },
    allApStates: { id: "party.allApStates", defaultMessage: "Všechny stavy" },
    apState: { id: "party.apState", defaultMessage: "Stav" },
    listItemsVisibleCountFrom: { id: "party.list.itemsVisibleCountFrom", defaultMessage: "Zobrazeno {0} z {1}, zpřesněte vyhledávací filtr" },
    recordScope: { id: "party.recordScope", defaultMessage: "Oblast entit" },
    addNewRegistry: { id: "registry.addNewRegistry", defaultMessage: "Nová entita" },
    addRegistry: { id: "registry.addRegistry", defaultMessage: "Nová archivní entita" },
    all: { id: "registry.all", defaultMessage: "Vše" },
    allRevisionStates: { id: "registry.allRevisionStates", defaultMessage: "Vše" },
    batchExportAction: { id: "registry.batchExport.action", defaultMessage: "Exportovat do CSV" },
    changeStateRevision: { id: "registry.changeStateRevision", defaultMessage: "Změnit vlastnosti revize" },
    createRevision: { id: "registry.createRevision", defaultMessage: "Vytvořit revizi" },
    createRevisionQuestion: { id: "registry.createRevisionQuestion", defaultMessage: "Opravdu chcete vytvořit revizi archivní entity?" },
    deleteRegistry: { id: "registry.deleteRegistry", defaultMessage: "Smazat" },
    deleteRegistryQuestion: { id: "registry.deleteRegistryQuestion", defaultMessage: "Opravdu chcete smazat archivní entitu?" },
    deleteRevision: { id: "registry.deleteRevision", defaultMessage: "Smazat revizi" },
    deleteRevisionQuestion: { id: "registry.deleteRevisionQuestion", defaultMessage: "Opravdu chcete smazat revizi archivní entity?" },
    listNoRecord: { id: "registry.list.noRecord", defaultMessage: "Zadanému filtru neodpovídají žádná data" },
    mergeRevision: { id: "registry.mergeRevision", defaultMessage: "Potvrzení revize" },
    moreActionsTitle: { id: "registry.moreActions.title", defaultMessage: "Další akce" },
    registryUsage: { id: "registry.registryUsage", defaultMessage: "Místa použití" },
    restoreEntity: { id: "registry.restoreEntity", defaultMessage: "Obnovit entitu" },
    revisionState: { id: "registry.revisionState", defaultMessage: "Stav revize" },
    actionApExtSearch: { id: "ribbon.action.ap.ext-search", defaultMessage: "Import ze systému" },
    actionApExtSyncs: { id: "ribbon.action.ap.ext-syncs", defaultMessage: "Fronta synchronizace" },
    actionRegistryImport: { id: "ribbon.action.registry.import", defaultMessage: "Import ze souboru" },
    actionRegistryScopeManage: { id: "ribbon.action.registry.scope.manage", defaultMessage: "Správa oblastí entit" },
});
