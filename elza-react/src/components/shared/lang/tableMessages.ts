import { defineMessages } from "react-intl";

/**
 * Popisky společné pro seznamy a tabulky napříč aplikací.
 *
 * Patří sem jen texty, které opravdu sdílí více seznamů - výběr sloupců, hromadný
 * výběr řádků a podobně. Texty vázané na konkrétní agendu zůstávají u ní.
 */
export const tableMessages = defineMessages({
    columns: {
        id: "table.filter.columns",
        defaultMessage: "Sloupce",
    },
    selectAll: {
        id: "table.selectAll",
        defaultMessage: "Vybrat vše",
    },
    select: {
        id: "table.select",
        defaultMessage: "Vybrat",
    },
});
