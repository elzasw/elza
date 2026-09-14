import { defineMessages } from "react-intl";

/**
 * Typy a popisy změn v historii pořádání.
 *
 * Klíč se dřív skládal z hodnoty ze serveru (`arr.history.change.title.` + typ),
 * což statický extraktor nevidí - klíče se do katalogu nikdy nedostaly. Množina
 * je uzavřená, takže je vypsaná a vybírá se přes `messageFor`.
 *
 * Id jsou převzatá z legacy katalogu beze změny. Placeholder {0} zůstává:
 * ICU bere jako jméno argumentu i číslo.
 */
export const historyChangeMessages = defineMessages({
    unknown: { id: "arr.history.change.title.unknown", defaultMessage: "neznámý" },
    CREATE_AS: { id: "arr.history.change.title.CREATE_AS", defaultMessage: "vytvoření AS" },
    ADD_NODES_OUTPUT: { id: "arr.history.change.title.ADD_NODES_OUTPUT", defaultMessage: "připojení JP k výstupu" },
    REMOVE_NODES_OUTPUT: { id: "arr.history.change.title.REMOVE_NODES_OUTPUT", defaultMessage: "odpojení JP od výstupu" },
    ADD_LEVEL: { id: "arr.history.change.title.ADD_LEVEL", defaultMessage: "založení JP" },
    MOVE_LEVEL: { id: "arr.history.change.title.MOVE_LEVEL", defaultMessage: "přesun JP" },
    DELETE_LEVEL: { id: "arr.history.change.title.DELETE_LEVEL", defaultMessage: "zrušení JP" },
    ADD_NODE_EXTENSION: { id: "arr.history.change.title.ADD_NODE_EXTENSION", defaultMessage: "přidání rozšíření k JP" },
    DELETE_NODE_EXTENSION: { id: "arr.history.change.title.DELETE_NODE_EXTENSION", defaultMessage: "zrušení rozšíření k JP" },
    SET_NODE_EXTENSION: { id: "arr.history.change.title.SET_NODE_EXTENSION", defaultMessage: "nastavení rozšíření k JP" },
    UPDATE_DESC_ITEM: { id: "arr.history.change.title.UPDATE_DESC_ITEM", defaultMessage: "změna atributu" },
    ADD_DESC_ITEM: { id: "arr.history.change.title.ADD_DESC_ITEM", defaultMessage: "založení atributu" },
    DELETE_DESC_ITEM: { id: "arr.history.change.title.DELETE_DESC_ITEM", defaultMessage: "zrušení atributu" },
    UPDATE_STRUCTURE_ITEM: { id: "arr.history.change.title.UPDATE_STRUCTURE_ITEM", defaultMessage: "změna atributu u strukt. typu" },
    ADD_STRUCTURE_ITEM: { id: "arr.history.change.title.ADD_STRUCTURE_ITEM", defaultMessage: "založení atributu u strukt. typu" },
    DELETE_STRUCTURE_ITEM: { id: "arr.history.change.title.DELETE_STRUCTURE_ITEM", defaultMessage: "zrušení atributu u strukt. typu" },
    ADD_STRUCTURE_DATA: { id: "arr.history.change.title.ADD_STRUCTURE_DATA", defaultMessage: "založení strukturovaného typu" },
    ADD_STRUCTURE_DATA_BATCH: { id: "arr.history.change.title.ADD_STRUCTURE_DATA_BATCH", defaultMessage: "hromadné založení strukt. typu" },
    UPDATE_STRUCT_DATA_BATCH: { id: "arr.history.change.title.UPDATE_STRUCT_DATA_BATCH", defaultMessage: "hromadná úprava strukt. typů" },
    DELETE_STRUCTURE_DATA: { id: "arr.history.change.title.DELETE_STRUCTURE_DATA", defaultMessage: "zrušení strukturovaného typu" },
    ADD_FUND_STRUCTURE_EXT: { id: "arr.history.change.title.ADD_FUND_STRUCTURE_EXT", defaultMessage: "přiřazení rozšíření k AS" },
    SET_FUND_STRUCTURE_EXT: { id: "arr.history.change.title.SET_FUND_STRUCTURE_EXT", defaultMessage: "nastavení přiřazení k AS" },
    DELETE_FUND_STRUCTURE_EXT: { id: "arr.history.change.title.DELETE_FUND_STRUCTURE_EXT", defaultMessage: "odebrání rozšíření u AS" },
    BATCH_CHANGE_DESC_ITEM: { id: "arr.history.change.title.BATCH_CHANGE_DESC_ITEM", defaultMessage: "hromadná změna atributů" },
    BATCH_DELETE_DESC_ITEM: { id: "arr.history.change.title.BATCH_DELETE_DESC_ITEM", defaultMessage: "hromadné vymazání atributů" },
    BULK_ACTION: { id: "arr.history.change.title.BULK_ACTION", defaultMessage: "hromadná funkce" },
    IMPORT: { id: "arr.history.change.title.IMPORT", defaultMessage: "import AS" },
    CREATE_DIGI_REQUEST: { id: "arr.history.change.title.CREATE_DIGI_REQUEST", defaultMessage: "požadavek na digitalizaci" },
    CREATE_DAO_REQUEST: { id: "arr.history.change.title.CREATE_DAO_REQUEST", defaultMessage: "požadavek na delimitaci/skartaci" },
    CREATE_REQUEST_QUEUE: { id: "arr.history.change.title.CREATE_REQUEST_QUEUE", defaultMessage: "vytvoření položky ve frontě" },
    CREATE_DAO_LINK: { id: "arr.history.change.title.CREATE_DAO_LINK", defaultMessage: "připojení DAO k JP" },
    DELETE_DAO_LINK: { id: "arr.history.change.title.DELETE_DAO_LINK", defaultMessage: "odpojení DAO od JP" },
    UPDATE_OUTPUT: { id: "arr.history.change.title.UPDATE_OUTPUT", defaultMessage: "úprava dat výstupu" },
    REPLACE_REGISTER: { id: "arr.history.change.title.REPLACE_REGISTER", defaultMessage: "náhrada přístupového bodu" },
    REPLACE_PARTY: { id: "arr.history.change.title.REPLACE_PARTY", defaultMessage: "party replace" },
    GENERATE_OUTPUT: { id: "arr.history.change.title.GENERATE_OUTPUT", defaultMessage: "generování výstupu" },
    SYNCHRONIZE_JP: { id: "arr.history.change.title.SYNCHRONIZE_JP", defaultMessage: "synchronizace JP" },
    CHANGE_SCENARIO_ITEMS: { id: "arr.history.change.title.CHANGE_SCENARIO_ITEMS", defaultMessage: "změna záznamu podle scénářů" },
    ADD_ATTACHMENT: { id: "arr.history.change.title.ADD_ATTACHMENT", defaultMessage: "přidání souboru" },
    DELETE_ATTACHMENT: { id: "arr.history.change.title.DELETE_ATTACHMENT", defaultMessage: "mazání souborů" },
});

/** Podrobnější popis změny; má ho jen část typů. */
export const historyDescriptionMessages = defineMessages({
    CREATE_AS: { id: "arr.history.change.description.CREATE_AS", defaultMessage: "Vytvoření archivního souboru" },
    ADD_NODES_OUTPUT: { id: "arr.history.change.description.ADD_NODES_OUTPUT", defaultMessage: "Připojení JP ({0}) k výstupu" },
    REMOVE_NODES_OUTPUT: { id: "arr.history.change.description.REMOVE_NODES_OUTPUT", defaultMessage: "Odpojení JP ({0}) od výstupu" },
    BATCH_CHANGE_DESC_ITEM: { id: "arr.history.change.description.BATCH_CHANGE_DESC_ITEM", defaultMessage: "Hromadná úprava hodnot atributů" },
    BATCH_DELETE_DESC_ITEM: { id: "arr.history.change.description.BATCH_DELETE_DESC_ITEM", defaultMessage: "Hromadný výmaz hodnot atributů" },
    BULK_ACTION: { id: "arr.history.change.description.BULK_ACTION", defaultMessage: "Funkce (Ovlivněno JP: {0})" },
    IMPORT: { id: "arr.history.change.description.IMPORT", defaultMessage: "Import do AS" },
    CREATE_DIGI_REQUEST: { id: "arr.history.change.description.CREATE_DIGI_REQUEST", defaultMessage: "Požadavek na digitalizaci" },
    CREATE_REQUEST_QUEUE: { id: "arr.history.change.description.CREATE_REQUEST_QUEUE", defaultMessage: "Vytvoření položky ve frontě" },
    CREATE_DAO_LINK: { id: "arr.history.change.description.CREATE_DAO_LINK", defaultMessage: "Vytvoření vazby na digitalizát" },
    DELETE_DAO_LINK: { id: "arr.history.change.description.DELETE_DAO_LINK", defaultMessage: "Zrušení vazby na digitalizát" },
});
