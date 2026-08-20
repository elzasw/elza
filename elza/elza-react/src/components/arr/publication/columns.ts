import { MessageDescriptor } from "react-intl";
import { PublicationDetail } from "elza-api";
import { messages } from "./messages";


export type PublicationColDef = {
    key: keyof PublicationDetail;
    message: MessageDescriptor;
    type: "string" | "date";
    minWidth: number;
    idealWidth: number;
    /**
     * Optional value extractor for non-scalar columns (e.g. `createdBy: UserRef`).
     * Returned string is used both for rendering and for sort comparison; falsy
     * results fall back to a placeholder in the table.
     */
    getValue?: (item: PublicationDetail) => string | undefined | null;
};

export const colDef: PublicationColDef[] = [
    { key: "typeName",      message: messages.typeName,      type: "string", minWidth: 100, idealWidth: 130 },
    { key: "state",         message: messages.state,         type: "string", minWidth: 70,  idealWidth: 100 },
    { key: "createdBy",     message: messages.createdBy,     type: "string", minWidth: 120, idealWidth: 180,
      getValue: (item) => item.createdBy ? `${item.createdBy.name} (${item.createdBy.username})` : undefined },
    { key: "createdAt",     message: messages.createdAt,     type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "preparedAt",    message: messages.preparedAt,    type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "publishedAt",   message: messages.publishedAt,   type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "lastFetchedAt", message: messages.lastFetchedAt, type: "date",   minWidth: 120, idealWidth: 150 },
    { key: "invalidatedAt", message: messages.invalidatedAt, type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "errorAt",       message: messages.errorAt,       type: "date",   minWidth: 100, idealWidth: 130 },
    { key: "errorMessage",  message: messages.errorMessage,  type: "string", minWidth: 120, idealWidth: 200 },
];
