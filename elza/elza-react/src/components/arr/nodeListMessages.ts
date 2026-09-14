import { defineMessages } from "react-intl";

/**
 * Popisky akcí nad výběrem jednotek popisu.
 *
 * Sdílí je `FundNodesList` a `ScopeList`. Dřív se předávaly do `AddRemoveList`
 * jako legacy klíče v řetězci (`addTitle="arr.fund.nodes.title.select"`);
 * prop nyní bere deskriptor, takže je klíč vidět staticky.
 *
 * Id jsou převzatá z legacy katalogu beze změny; opraven jen překlep
 * "jednoteku" -> "jednotku".
 */
export const nodeListMessages = defineMessages({
    select: {
        id: "arr.fund.nodes.title.select",
        defaultMessage: "Výběr jednotek popisu",
    },
    remove: {
        id: "arr.fund.nodes.title.remove",
        defaultMessage: "Odebrat jednotku popisu z výběru",
    },
});
