import { defineMessages } from "react-intl";

/**
 * Popisky filtrů archivních entit.
 *
 * Nadpis sekce se dřív předával propem `name` jako legacy klíč v řetězci.
 * Prop se jmenoval stejně jako `name` u `FormSection` a `Field`, kde ale
 * znamená název pole formuláře - dvě různé věci pod jedním jménem. Nadpis se
 * proto nově jmenuje `sectionTitle` a nese deskriptor.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const filterMessages = defineMessages({
    sectionBase: { id: "ap.ext-search.section.base", defaultMessage: "Obecný filtr" },
    sectionCreExt: { id: "ap.ext-search.section.cre-ext", defaultMessage: "Vznik/Zánik" },
    sectionText: { id: "ap.ext-search.section.text", defaultMessage: "Textové vyhledávání" },
    sectionExtSystems: { id: "ap.ext-search.section.ext-systems", defaultMessage: "Externí systémy" },
    sectionExtends: {
        id: "ap.ext-search.section.extends.title",
        defaultMessage: "Přidat rozšířený filtr",
    },
    sectionRelations: {
        id: "ap.ext-search.section.relations.title",
        defaultMessage: "Přidat filtr vztahů",
    },
    type: { id: "registry.type", defaultMessage: "Třída / podtřída entity" },
    id: { id: "ap.ext-search.id", defaultMessage: "ID" },
    user: { id: "ap.ext-search.user", defaultMessage: "Autor poslední změny" },
    assignedTo: { id: "ap.ext-search.assignedTo", defaultMessage: "Přiděleno" },
    syncState: { id: "ap.ext-search.syncState", defaultMessage: "Stav synchronizace" },
    validationResult: { id: "ap.ext-search.validationResult", defaultMessage: "Stav validace" },
    creation: { id: "ap.ext-search.creation", defaultMessage: "Datum vzniku" },
    extinction: { id: "ap.ext-search.extinction", defaultMessage: "Datum zániku" },
    extSystem: { id: "ap.ext-search.ext-system", defaultMessage: "Externí systém" },
    selectAll: { id: "ap.ext-search.input.select.all", defaultMessage: "Vše" },
    search: { id: "ap.ext-search.search", defaultMessage: "Vyhledávání" },
    area: { id: "ap.ext-search.area", defaultMessage: "Oblast hledání" },
    onlyMainPart: { id: "ap.ext-search.only-main-part", defaultMessage: "Pouze hlavní část" },
    // Podrobnosti filtrů; sdílí je modaly rozšířeného filtru a filtru vztahů.
    extendsValue: { id: "ap.ext-search.section.extends.value", defaultMessage: "Hodnota" },
    extendsSpec: { id: "ap.ext-search.section.extends.spec", defaultMessage: "Specifikace" },
    extendsPart: { id: "ap.ext-search.section.extends.part", defaultMessage: "Část popisu" },
    extendsType: { id: "ap.ext-search.section.extends.type", defaultMessage: "Typ prvku popisu" },
    relationsType: { id: "ap.ext-search.section.relations.type", defaultMessage: "Typ vztahu" },
    relationsSpec: { id: "ap.ext-search.section.relations.spec", defaultMessage: "Specifikace" },
    relationsArea: { id: "ap.ext-search.section.relations.area", defaultMessage: "Oblast hledání" },
    relationsObj: { id: "ap.ext-search.section.relations.obj", defaultMessage: "Archivní entita" },
    relationsOnlyMainPart: {
        id: "ap.ext-search.section.relations.only-main-part",
        defaultMessage: "Pouze hlavní část",
    },
});

/** Stav synchronizace s externím systémem; klíč se dřív skládal z hodnoty. */
export const syncStateMessages = defineMessages({
    SYNC_OK: { id: "ap.binding.syncState.SYNC_OK", defaultMessage: "Aktivní synchronizace" },
    NOT_SYNCED: { id: "ap.binding.syncState.NOT_SYNCED", defaultMessage: "Nesynchronizuje se" },
    LOCAL_CHANGE: { id: "ap.binding.syncState.LOCAL_CHANGE", defaultMessage: "Existuje lokální změna" },
});

/** Výsledek validace entity; klíč se dřív skládal z hodnoty. */
export const validationResultMessages = defineMessages({
    ok: { id: "ap.ext-search.validationResult.ok", defaultMessage: "OK" },
    error: { id: "ap.ext-search.validationResult.error", defaultMessage: "Chyba" },
});
