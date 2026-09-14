import { defineMessages } from "react-intl";

/**
 * Titulky modálů pro zakládání a úpravu částí archivní entity.
 *
 * Klíče map jsou **hodnoty** enumu `PartType` (`PT_BODY`, …), ne jeho členy -
 * podle nich se vybírá za běhu. Id zůstávají čitelná.
 *
 * Hodnota se předává do `modalDialogShow`, tedy mimo React strom, a musí být
 * řetězec - proto se formátuje přes `getIntl()`, ne přes `<FormattedMessage>`.
 */
export const partCreateMessages = defineMessages({
    PT_BODY: { id: "ap.part.create.BODY", defaultMessage: "Nové tělo" },
    PT_CRE: { id: "ap.part.create.CRE", defaultMessage: "Nový vznik" },
    PT_EVENT: { id: "ap.part.create.EVENT", defaultMessage: "Nová událost" },
    PT_EXT: { id: "ap.part.create.EXT", defaultMessage: "Nový zánik" },
    PT_IDENT: { id: "ap.part.create.IDENT", defaultMessage: "Nový identifikátor" },
    PT_NAME: { id: "ap.part.create.NAME", defaultMessage: "Nové označení" },
    PT_REL: { id: "ap.part.create.REL", defaultMessage: "Nový vztah" },
});

export const partEditMessages = defineMessages({
    PT_BODY: { id: "ap.part.edit.BODY", defaultMessage: "Upravit tělo" },
    PT_CRE: { id: "ap.part.edit.CRE", defaultMessage: "Upravit vznik" },
    PT_EVENT: { id: "ap.part.edit.EVENT", defaultMessage: "Upravit událost" },
    PT_EXT: { id: "ap.part.edit.EXT", defaultMessage: "Upravit zánik" },
    PT_IDENT: { id: "ap.part.edit.IDENT", defaultMessage: "Upravit identifikátor" },
    PT_NAME: { id: "ap.part.edit.NAME", defaultMessage: "Upravit označení" },
    PT_REL: { id: "ap.part.edit.REL", defaultMessage: "Upravit vztah" },
});
