import { defineMessages, MessageDescriptor } from "react-intl";
import { PublicationDetail, PublicationStateInternal } from "elza-api";

export const messages = defineMessages({
    typeName:         { id: "publication.col.typeName",         defaultMessage: "Systém" },
    state:            { id: "publication.col.state",            defaultMessage: "Stav" },
    createdByUserId:  { id: "publication.col.createdByUserId",  defaultMessage: "Uživatel" },
    createdAt:        { id: "publication.col.createdAt",        defaultMessage: "Vytvořeno" },
    preparedAt:       { id: "publication.col.preparedAt",       defaultMessage: "Připraveno" },
    errorAt:          { id: "publication.col.errorAt",          defaultMessage: "Čas chyby" },
    lastFetchedAt:    { id: "publication.col.lastFetchedAt",    defaultMessage: "Naposledy staženo" },
    publishedAt:      { id: "publication.col.publishedAt",      defaultMessage: "Publikováno" },
    invalidatedAt:    { id: "publication.col.invalidatedAt",    defaultMessage: "Zneplatněno" },
    errorMessage:     { id: "publication.col.errorMessage",     defaultMessage: "Chybová zpráva" },
});

export const stateMessages = defineMessages({
    [PublicationStateInternal.New]:          { id: `publication.state.${PublicationStateInternal.New}`,          defaultMessage: "Nová" },
    [PublicationStateInternal.Prepared]:     { id: `publication.state.${PublicationStateInternal.Prepared}`,     defaultMessage: "Připravená" },
    [PublicationStateInternal.Fetched]:      { id: `publication.state.${PublicationStateInternal.Fetched}`,      defaultMessage: "Stažená" },
    [PublicationStateInternal.Published]:    { id: `publication.state.${PublicationStateInternal.Published}`,    defaultMessage: "Publikovaná" },
    [PublicationStateInternal.PrepareError]: { id: `publication.state.${PublicationStateInternal.PrepareError}`, defaultMessage: "Chyba přípravy" },
    [PublicationStateInternal.PublishError]: { id: `publication.state.${PublicationStateInternal.PublishError}`, defaultMessage: "Chyba publikace" },
    [PublicationStateInternal.Invalidated]:  { id: `publication.state.${PublicationStateInternal.Invalidated}`,  defaultMessage: "Zneplatněná" },
});

export const colDef: { key: keyof PublicationDetail; message: MessageDescriptor; type: "string" | "date"; minWidth: number; idealWidth: number }[] = [
    { key: "typeName",        message: messages.typeName,        type: "string", minWidth: 100, idealWidth: 130 },
    { key: "state",           message: messages.state,           type: "string", minWidth: 70,  idealWidth: 100 },
    { key: "createdByUserId", message: messages.createdByUserId, type: "string", minWidth: 100, idealWidth: 130 },
    { key: "createdAt",       message: messages.createdAt,       type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "preparedAt",      message: messages.preparedAt,      type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "publishedAt",     message: messages.publishedAt,     type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "lastFetchedAt",   message: messages.lastFetchedAt,   type: "date",   minWidth: 120, idealWidth: 150 },
    { key: "invalidatedAt",   message: messages.invalidatedAt,   type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "errorAt",         message: messages.errorAt,         type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "errorMessage",    message: messages.errorMessage,    type: "string", minWidth: 120, idealWidth: 200 },
];
