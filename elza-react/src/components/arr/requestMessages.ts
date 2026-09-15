import { defineMessages } from "react-intl";

/**
 * Popisky pozadavku na digitalizaci, delimitaci a skartaci a formulare
 * pro vytvoreni vystupu.
 * 
 * Generovano primo z legacy katalogu, aby pri prepisu nevznikl preklep.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const requestMessages = defineMessages({
    fundNodesDeleteNode: { id: "arr.fund.nodes.deleteNode", defaultMessage: "Opravdu chcete smazat tuto položku?" },
    fundNodesTitleSelect: { id: "arr.fund.nodes.title.select", defaultMessage: "Výběr jednotek popisu" },
    outputInternalCode: { id: "arr.output.internalCode", defaultMessage: "Interní kód výstupu" },
    outputName: { id: "arr.output.name", defaultMessage: "Název výstupu" },
    outputNoSelectionMessage: { id: "arr.output.noSelection.message", defaultMessage: "Prosím vyberte výstup ze seznamu nebo vytvořte nový. Pro vytvoření výstupu je potřeba povolit úpravy." },
    outputNoSelectionTitle: { id: "arr.output.noSelection.title", defaultMessage: "Není vybrán výstup" },
    outputOutputFilter: { id: "arr.output.outputFilter", defaultMessage: "Výstupní filtr" },
    outputOutputType: { id: "arr.output.outputType", defaultMessage: "Typ výstupu" },
    outputTemplate: { id: "arr.output.template", defaultMessage: "Šablona" },
    requestActionStoreAndSend: { id: "arr.request.action.storeAndSend", defaultMessage: "Uložit a odeslat" },
    requestNoSelectionMessage: { id: "arr.request.noSelection.message", defaultMessage: "Prosím vyberte požadavek na digitalizaci ze seznamu." },
    requestNoSelectionTitle: { id: "arr.request.noSelection.title", defaultMessage: "Není vybrán požadavek na digitalizaci" },
    requestTitleCreated: { id: "arr.request.title.created", defaultMessage: "Vytvořen" },
    requestTitleDaoRequestDigitizationFrontdesk: { id: "arr.request.title.daoRequest.digitizationFrontdesk", defaultMessage: "Digitalizační linka" },
    requestTitleDaoRequestIdentifiersCode: { id: "arr.request.title.daoRequest.identifiers.code", defaultMessage: "Interní kód" },
    requestTitleDaoRequestIdentifiersExternalCode: { id: "arr.request.title.daoRequest.identifiers.externalCode", defaultMessage: "Kód externího systému" },
    requestTitleDaoRequestSystem: { id: "arr.request.title.daoRequest.system", defaultMessage: "Ext. systém" },
    requestTitleDaoRequestType: { id: "arr.request.title.daoRequest.type", defaultMessage: "Typ požadavku" },
    requestTitleDescription: { id: "arr.request.title.description", defaultMessage: "Komentář" },
    requestTitleDigitizationRequest: { id: "arr.request.title.digitizationRequest", defaultMessage: "Požadavek na digitalizát" },
    requestTitleNewRequest: { id: "arr.request.title.newRequest", defaultMessage: "Nový" },
    requestTitleNodes: { id: "arr.request.title.nodes", defaultMessage: "Seznam jednotek popisu" },
    requestTitleNodesDaosWithoutNode: { id: "arr.request.title.nodes.daosWithoutNode", defaultMessage: "Bez JP" },
    requestTitleQueued: { id: "arr.request.title.queued", defaultMessage: "Ve frontě od" },
    requestTitleRejectReason: { id: "arr.request.title.rejectReason", defaultMessage: "Důvod odmítnutí" },
    requestTitleRequest: { id: "arr.request.title.request", defaultMessage: "Požadavek" },
    requestTitleSend: { id: "arr.request.title.send", defaultMessage: "Odeslán" },
    requestTitleTrysend: { id: "arr.request.title.trysend", defaultMessage: "Poslední pokus odeslání" },
    requestTitleType: { id: "arr.request.title.type", defaultMessage: "Typ" },
    requestTitleTypeDAO: { id: "arr.request.title.type.DAO", defaultMessage: "Požadavek na skartaci/delimitaci" },
    requestTitleTypeDAO_LINK: { id: "arr.request.title.type.DAO_LINK", defaultMessage: "Požadavek na připojení k/odpojení od JP" },
    requestTitleTypeDIGITIZATION: { id: "arr.request.title.type.DIGITIZATION", defaultMessage: "Požadavek na digitalizaci" },
    requestTitleTypeDaoDESTRUCTION: { id: "arr.request.title.type.dao.DESTRUCTION", defaultMessage: "Požadavek na skartaci" },
    requestTitleTypeDaoTRANSFER: { id: "arr.request.title.type.dao.TRANSFER", defaultMessage: "Požadavek na delimitaci" },
});

/** Druh pozadavku; klic se driv skladal z navratove hodnoty getRequestType(). */
export const requestTypeMessages = {
    DIGITIZATION: requestMessages.requestTitleTypeDIGITIZATION,
    DAO: requestMessages.requestTitleTypeDAO,
    DAO_LINK: requestMessages.requestTitleTypeDAO_LINK,
};

/** Podtyp pozadavku nad digitalnimi entitami. */
export const daoRequestTypeMessages = {
    DESTRUCTION: requestMessages.requestTitleTypeDaoDESTRUCTION,
    TRANSFER: requestMessages.requestTitleTypeDaoTRANSFER,
};
