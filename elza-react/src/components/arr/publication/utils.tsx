import { defineMessages, MessageDescriptor } from "react-intl";
import { PublicationDetail } from "elza-api";

export const messages = defineMessages({
    typeName:         { id: "publication.col.typeName",         defaultMessage: "Typ publikace" },
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

export const colDef: { key: keyof PublicationDetail; message: MessageDescriptor; type: string; minWidth: number; idealWidth: number }[] = [
    { key: "typeName",        message: messages.typeName,        type: "string", minWidth: 100, idealWidth: 130 },
    { key: "state",           message: messages.state,           type: "string", minWidth: 70,  idealWidth: 100 },
    { key: "createdByUserId", message: messages.createdByUserId, type: "string", minWidth: 100, idealWidth: 130 },
    { key: "createdAt",       message: messages.createdAt,       type: "string", minWidth: 100, idealWidth: 130 },
    { key: "preparedAt",      message: messages.preparedAt,      type: "string", minWidth: 100, idealWidth: 130 },
    { key: "publishedAt",     message: messages.publishedAt,     type: "string", minWidth: 100, idealWidth: 130 },
    { key: "lastFetchedAt",   message: messages.lastFetchedAt,   type: "string", minWidth: 120, idealWidth: 150 },
    { key: "invalidatedAt",   message: messages.invalidatedAt,   type: "string", minWidth: 100, idealWidth: 130 },
    { key: "errorAt",         message: messages.errorAt,         type: "string", minWidth: 100, idealWidth: 130 },
    { key: "errorMessage",    message: messages.errorMessage,    type: "string", minWidth: 120, idealWidth: 200 },
];
