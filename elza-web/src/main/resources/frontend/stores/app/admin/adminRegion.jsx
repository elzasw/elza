/**
 * Store pro sekci Administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import * as types from 'actions/constants/ActionTypes.js';
import packages from './packages.jsx'
import user from './user.jsx'
import group from './group.jsx'
import fulltext from './fulltext.jsx'
import {isUserAction} from 'actions/admin/user.jsx'
import {isGroupAction} from 'actions/admin/group.jsx'

/**
 * Výchozí stav store
 */
const initialState = {

    // seznam importovaných balíčků
    packages: packages(),
    fulltext: fulltext(),
    user: user(),
    group: group(),
}

export default function adminRegion(state = initialState, action = {}) {
    if (isUserAction(action)) {
        return {
            ...state,
            user: user(state.user, action)
        }
    } else if (isGroupAction(action)) {
        return {
            ...state,
            group: group(state.group, action)
        }
    }

    switch (action.type) {
        case types.STORE_LOAD:
            console.log("ADMIN region LOAD", action.adminRegion)
            if (action.adminRegion) {
                return {
                    ...state,
                    ...action.adminRegion,
                    packages: packages(),
                    fulltext: fulltext(),
                    user: user(action.adminRegion.user, action),
                    group: group(action.adminRegion.group, action),
                }
            }
        case types.STORE_SAVE:
            // const {activeIndex, nodeSettings, extendedView} = state;
            return {
                user: user(state.user, action),
                group: group(state.group, action),
            }
        case types.ADMIN_PACKAGES_REQUEST:
        case types.ADMIN_PACKAGES_RECEIVE:
        case types.ADMIN_PACKAGES_DELETE_RECEIVE:
        case types.ADMIN_PACKAGES_IMPORT_RECEIVE:
            return Object.assign({}, state, {
                packages: packages(state.packages, action)
            });
        case types.ADMIN_FULLTEXT_REINDEXING_REQUEST:
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST:
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_RECEIVE:
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

