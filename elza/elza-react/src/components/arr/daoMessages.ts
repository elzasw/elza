import { defineMessages } from "react-intl";

/**
 * Popisky digitalnich entit, prirazovani AIP, lektorovani a drobnych
 * panelu nad archivnim popisem.
 * 
 * Generovano primo z legacy katalogu, aby pri prepisu nevznikl preklep.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const daoMessages = defineMessages({
    aipDetailAssignmentDescription: { id: "aip.detail.assignment.description", defaultMessage: "Popis" },
    aipDetailAssignmentName: { id: "aip.detail.assignment.name", defaultMessage: "Název" },
    aipDetailAssignmentPackages: { id: "aip.detail.assignment.packages", defaultMessage: "Seznam balíčků" },
    aipDetailAssignmentPackagesNo: { id: "aip.detail.assignment.packagesNo", defaultMessage: "Počet balíčků" },
    aipDetailAssignmentRelatedAip: { id: "aip.detail.assignment.relatedAip", defaultMessage: "Související AIP" },
    aipDetailAssignmentRelatedAips: { id: "aip.detail.assignment.relatedAips", defaultMessage: "Související AIPy" },
    aipAssignmentBulk: { id: "arr.aip.assignment.bulk", defaultMessage: "Připojit hromadně" },
    aipAssignmentBulkTitle: { id: "arr.aip.assignment.bulk.title", defaultMessage: "Hromadné připojení balíčku s archivním popisem" },
    aipAssignmentCreate: { id: "arr.aip.assignment.create", defaultMessage: "Vytvořit JP z vybraných" },
    aipAssignmentCreateAndLink: { id: "arr.aip.assignment.create-and-link", defaultMessage: "Vytvořit JP s propojením" },
    aipAssignmentIndividually: { id: "arr.aip.assignment.individually", defaultMessage: "Připojit jednotlivě" },
    aipAssignmentIndividuallyTitle: { id: "arr.aip.assignment.individually.title", defaultMessage: "Připojení balíčku s archivním popisem" },
    aipAssignmentLink: { id: "arr.aip.assignment.link", defaultMessage: "Připojit k JP" },
    aipAssignmentPartConfirm: { id: "arr.aip.assignment.part.confirm", defaultMessage: "Opravdu chcete připojit pouze vybranou část? Touto volbou nedojde k propojení návazných částí balíčku." },
    aipAssignmentSelectAndCreate: { id: "arr.aip.assignment.select-and-create", defaultMessage: "Výběrové připojení s JP" },
    daosFileSystemLoadMore: { id: "arr.daos.fileSystem.loadMore", defaultMessage: "Načíst další..." },
    daosFileSystemSelectParent: { id: "arr.daos.fileSystem.selectParent", defaultMessage: "Přejít o úroveň výš" },
    fundAddTemplateCreate: { id: "arr.fund.addTemplate.create", defaultMessage: "Vytvoření šablony" },
    fundUseTemplateTitle: { id: "arr.fund.useTemplate.title", defaultMessage: "Použít šablonu" },
    issuesAddArr: { id: "arr.issues.add.arr", defaultMessage: "Archivní soubor" },
    issuesAddArrTitle: { id: "arr.issues.add.arr.title", defaultMessage: "Přidat připomínku - Archivní soubor" },
    issuesAddDeletedLevel: { id: "arr.issues.add.deletedLevel", defaultMessage: "smazaná JP" },
    issuesAddNode: { id: "arr.issues.add.node", defaultMessage: "Zobrazená jednotka popisu" },
    issuesAddNodeTitle: { id: "arr.issues.add.node.title", defaultMessage: "Přidat připomínku - Jednotka popisu" },
    issuesChoose: { id: "arr.issues.choose", defaultMessage: "Vyberte připomínku" },
    issuesSettingsTitle: { id: "arr.issues.settings.title", defaultMessage: "Nastavení lektorování" },
    issuesStateChange: { id: "arr.issues.state.change", defaultMessage: "změnit stav..." },
    issuesTypeChange: { id: "arr.issues.type.change", defaultMessage: "Změnit druh: {0}" },
    issuesUpdateTitle: { id: "arr.issues.update.title", defaultMessage: "Úprava připomínky" },
    linkedNodesTitle: { id: "arr.linked-nodes.title", defaultMessage: "Odkazující JP" },
    requestTitleDescription: { id: "arr.request.title.description", defaultMessage: "Komentář" },
    developerTitleDescItems: { id: "developer.title.descItems", defaultMessage: "Prvky popisu" },
    globalAll: { id: "global.all", defaultMessage: "Vše" },
    searchInputSearch: { id: "search.input.search", defaultMessage: "Vyhledat..." },
    subNodeDaoDaoActionChangeScenario: { id: "subNodeDao.dao.action.changeScenario", defaultMessage: "Způsob napojení" },
    subNodeDaoDaoActionShowDetailAll: { id: "subNodeDao.dao.action.showDetailAll", defaultMessage: "Zobrazit detail digitálních entit" },
    subNodeDaoDaoActionShowDetailOne: { id: "subNodeDao.dao.action.showDetailOne", defaultMessage: "Zobrazit detail digitální entity" },
    subNodeDaoDaoFilesTruncated: { id: "subNodeDao.dao.files.truncated", defaultMessage: "Zobrazeno pouze prvních 1000 souborů" },
    subNodeDaoTitle: { id: "subNodeDao.title", defaultMessage: "Digitální entity" },
    textFragmentsTitle: { id: "textFragments.title", defaultMessage: "Speciální znaky" },
    /**
     * Puvodne tri klice subNodeDao.dao.files.{one,few,more} vybirane rucni
     * funkci getPlurality() s natvrdo zapsanymi ceskymi pravidly - anglictina
     * kategorii "few" nema, takze to byla v EN realna chyba. Nahrazeno ICU
     * pluralem, ktery si kategorie resi podle locale.
     *
     * Vedlejsi efekt: "#" formatuje cislo podle locale, takze 1000 se v cestine
     * vypise jako "1 000".
     */
    daoFileCount: {
        id: "subNodeDao.dao.fileCount",
        defaultMessage: "{count, plural, one {# soubor} few {# soubory} other {# souborů}}",
    },
    daoFileCountTruncated: {
        id: "subNodeDao.dao.fileCountTruncated",
        defaultMessage: "{count, plural, one {#+ soubor} few {#+ soubory} other {#+ souborů}}",
    },
});
