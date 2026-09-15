import { defineMessages } from "react-intl";

/**
 * Způsoby vyhledávání archivních entit a uživatelů.
 *
 * Sdílí je `registry/ApField` a `admin/UserAndGroupField`. V obou se dřív
 * skládala mapa naformátovaných řetězců už při načtení modulu, tedy dávno před
 * tím, než je znám jazyk - po přepnutí by popisky zůstaly v původním znění.
 *
 * Id jsou převzatá z legacy katalogu beze změny.
 */
export const searchTypeMessages = defineMessages({
    createFulltext: {
        id: "apField.searchType.create.FULLTEXT",
        defaultMessage: "fulltextové vyhledání",
    },
    createRightLike: {
        id: "apField.searchType.create.RIGHT_LIKE",
        defaultMessage: "pravostranné hledání",
    },
    username: { id: "apField.searchType.USERNAME", defaultMessage: "vyhledání dle username" },
    usernameAndParty: {
        id: "apField.searchType.USERNAME_AND_PARTY",
        defaultMessage: "vyhledání dle username i dle osoby",
    },
    partyRightLike: {
        id: "apField.searchType.PARTY_RIGHT_LIKE",
        defaultMessage: "vyhledání dle osoby - pravostranné",
    },
    partyFulltext: {
        id: "apField.searchType.PARTY_FULLTEXT",
        defaultMessage: "vyhledání dle osoby - fulltext",
    },
    group: { id: "userAndGroupField.searchType.GROUP", defaultMessage: "vyhledání skupiny" },
    usernameAndGroups: {
        id: "userAndGroupField.searchType.USERNAME_AND_GROUPS",
        defaultMessage: "vyhledání dle username a skupiny",
    },
});
