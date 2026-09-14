import { defineMessages } from "react-intl";

/** Popisky modálů nad archivními entitami. */
export const registryModalMessages = defineMessages({
    searchArea: { id: "ap.modal.searchArea", defaultMessage: "Oblast hledání" },
    unsupportedType: { id: "ap.modal.unsupportedType", defaultMessage: "Nepodporovaný typ" },
    relatedEntityRequired: {
        id: "ap.modal.relatedEntityRequired",
        defaultMessage: "Návazná archivní entita je povinná",
    },
});
