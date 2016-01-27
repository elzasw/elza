/**
 * Store pro sekci Administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import * as types from 'actions/constants/actionTypes';
import packages from './packages'
import fulltext from './fulltext'

/**
 * Výchozí stav store
 */
const initialState = {

    // seznam importovaných balíčků
    packages: packages(),
    fulltext: fulltext()
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
        case types.ADMIN_FULLTEXT_REINDEXING_REQUEST:
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST:
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_RECIEVE:
            return Object.assign({}, state, {
                fulltext: fulltext(state.fulltext, action)
            });

        case types.CHANGE_INDEXING_FINISHED:

            var fulltextChange = fulltext(state.fulltext, action);

            if (fulltextChange !== state.fulltext) {
                return Object.assign({}, state, {
                    fulltext: fulltextChange
                });
            }

            return state;

        case types.CHANGE_PACKAGE:

            var packagesChange = packages(state.packages, action);

            if (packagesChange !== state.packages) {
                return Object.assign({}, state, {
                    packages: packagesChange
                });
            }

            return state;

        default:
            return state;
    }
}

