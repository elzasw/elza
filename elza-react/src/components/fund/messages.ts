import { defineMessages } from "react-intl";

/**
 * Akce nad archivními soubory sdílené mezi ribbony.
 *
 * Stejné popisky nese ribbon na domovské stránce (`components/home/HomePageRibbon`)
 * i na přehledu fondů (`components/fund/FundPageRibbon`). Jedno id na dvou
 * místech projde extrakcí jen dokud se `defaultMessage` shodují, takže je
 * spolehlivější mít deskriptor jeden a importovat ho.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const fundMessages = defineMessages({
    add: {
        id: "ribbon.action.arr.fund.add",
        defaultMessage: "Nový AS",
    },
    search: {
        id: "ribbon.action.arr.fund.search",
        defaultMessage: "Vyhledat v arch. souborech",
    },
    import: {
        id: "ribbon.action.arr.fund.import",
        defaultMessage: "Import ze souboru",
    },
    publicationSystems: {
        id: "ribbon.action.arr.fund.publicationSystems",
        defaultMessage: "Správa publikačních systémů",
    },
});
