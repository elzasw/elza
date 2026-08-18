import { defineMessages } from "react-intl";
import { PublicationStateInternal } from "elza-api";


export const messages = defineMessages({
    typeName:      { id: "publication.col.typeName",      defaultMessage: "Systém" },
    state:         { id: "publication.col.state",         defaultMessage: "Stav" },
    createdBy:     { id: "publication.col.createdBy",     defaultMessage: "Uživatel" },
    createdAt:     { id: "publication.col.createdAt",     defaultMessage: "Vytvořeno" },
    preparedAt:    { id: "publication.col.preparedAt",    defaultMessage: "Připraveno" },
    errorAt:       { id: "publication.col.errorAt",       defaultMessage: "Čas chyby" },
    lastFetchedAt: { id: "publication.col.lastFetchedAt", defaultMessage: "Převzato" },
    publishedAt:   { id: "publication.col.publishedAt",   defaultMessage: "Publikováno" },
    invalidatedAt: { id: "publication.col.invalidatedAt", defaultMessage: "Zneplatněno" },
    errorMessage:  { id: "publication.col.errorMessage",  defaultMessage: "Chybová zpráva" },
});

export const stateMessages = defineMessages({
    [PublicationStateInternal.New]:          { id: "publication.state.NEW",           defaultMessage: "Nová" },
    [PublicationStateInternal.Prepared]:     { id: "publication.state.PREPARED",      defaultMessage: "Připravená" },
    [PublicationStateInternal.Fetched]:      { id: "publication.state.FETCHED",       defaultMessage: "Stažená" },
    [PublicationStateInternal.Published]:    { id: "publication.state.PUBLISHED",     defaultMessage: "Publikovaná" },
    [PublicationStateInternal.PrepareError]: { id: "publication.state.PREPARE_ERROR", defaultMessage: "Chyba přípravy" },
    [PublicationStateInternal.PublishError]: { id: "publication.state.PUBLISH_ERROR", defaultMessage: "Chyba publikace" },
    [PublicationStateInternal.Invalidated]:  { id: "publication.state.INVALIDATED",   defaultMessage: "Zneplatněná" },
});
