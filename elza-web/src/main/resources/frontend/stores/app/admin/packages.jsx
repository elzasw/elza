/**
 * Store pro správu importovaných balíčků.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import * as types from 'actions/constants/actionTypes';

/**
 * Výchozí stav store
 */
const initialState = {
    isFetching: false,
    fetched: false,
    items : []
}

export default function packages(state = initialState, action = {}) {
    switch (action.type) {

        case types.ADMIN_PACKAGES_REQUEST:
            return Object.assign({}, state, {
                isFetching: true
            })

        case types.ADMIN_PACKAGES_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                items: action.items
            })

        case types.ADMIN_PACKAGES_DELETE_RECEIVE:
        case types.ADMIN_PACKAGES_IMPORT_RECEIVE:
            return Object.assign({}, state, {
                fetched: false
            })

        default:
            return state
    }
}

