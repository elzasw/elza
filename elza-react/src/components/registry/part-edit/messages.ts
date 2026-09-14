import { defineMessages } from "react-intl";

/**
 * Popisky editace casti archivni entity.
 * 
 * Generovano primo z legacy katalogu, aby pri prepisu nevznikl preklep.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const partEditMessages = defineMessages({
    coordinateEditInMap: { id: "ap.coordinate.edit-in-map", defaultMessage: "Upravit v mapě" },
    coordinateImportTitle: { id: "ap.coordinate.import.title", defaultMessage: "Importovat souřadnice" },
    coordinateMapEditorTitle: { id: "ap.coordinate.map-editor.title", defaultMessage: "Editor souřadnic" },
    partComplementsCreate: { id: "ap.part.complements.create", defaultMessage: "Vytvořit doplňky" },
    partComplementsNoChangesMessage: { id: "ap.part.complements.noChanges.message", defaultMessage: "Doplňky jsou již vytvořeny." },
    partComplementsNoChangesTitle: { id: "ap.part.complements.noChanges.title", defaultMessage: "Existující hodnoty" },
    partComplementsNoItemsMessage: { id: "ap.part.complements.noItems.message", defaultMessage: "Nebylo možné vytvořit doplňky." },
    partComplementsNoItemsTitle: { id: "ap.part.complements.noItems.title", defaultMessage: "Doplňky nevytvořeny" },
    formValidationErrors: { id: "ap.partEdit.validationErrors", defaultMessage: "Chyby validace formuláře." },
    uriRefInvalidFormat: { id: "ap.partEdit.uriRef.invalidFormat", defaultMessage: "Nesprávný formát odkazu" },
    uriRefDescription: { id: "ap.partEdit.uriRef.description", defaultMessage: "Název" },
});
