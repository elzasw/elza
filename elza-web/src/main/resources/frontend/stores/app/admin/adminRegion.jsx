/**
 * Store pro sekci Administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import * as types from 'actions/constants/actionTypes';
import packages from './packages'

/**
 * Výchozí stav store
 */
const initialState = {

    // seznam importovaných balíčků
    packages: packages()

}

export default function adminRegion(state = initialState, action = {}) {
    switch (action.type) {

        case types.ADMIN_PACKAGES_REQUEST:
        case types.ADMIN_PACKAGES_RECEIVE:
        case types.ADMIN_PACKAGES_DELETE_RECEIVE:
        case types.ADMIN_PACKAGES_IMPORT_RECEIVE:
            return Object.assign({}, state, {
                packages: packages(state.packages, action)
            });

        default:
            return state;
    }
}

