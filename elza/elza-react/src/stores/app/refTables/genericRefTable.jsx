export const genericRefTableState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: [],
};

/**
 * Generická ref tabulka
 *
 * @param request Typ akce request
 * @param receive Typ akce receive
 * @param state state storu
 * @param action akce
 * @returns {*}
 */
export default function genericRefTable(request, receive, state, action) {
    switch (action.type) {
        case request:
            return {
                ...state,
                isFetching: true,
            };
        case receive:
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt,
            };
        default:
            return state;
    }
}
