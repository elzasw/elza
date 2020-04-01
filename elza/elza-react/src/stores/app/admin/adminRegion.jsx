/**
 * Store pro sekci Administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import * as types from 'actions/constants/ActionTypes.js';
import packages from './packages.jsx';
import user from './user.jsx';
import group from './group.jsx';
import fulltext from './fulltext.jsx';
import {isUserAction} from 'actions/admin/user.jsx';
import {isGroupAction} from 'actions/admin/group.jsx';
import {isPermissionAction} from 'actions/admin/permission.jsx';
import DetailReducer from 'shared/detail/DetailReducer';
import processAreaStores from '../../../shared/utils/processAreaStores';
import SimpleListReducer from '../../../shared/list/simple/SimpleListReducer';

/**
 * Výchozí stav store
 */
const initialState = {
    // seznam importovaných balíčků
    packages: packages(),
    fulltext: fulltext(),
    user: user(),
    group: group(),
    funds: SimpleListReducer(),
    fund: DetailReducer(),
    entityPermissions: DetailReducer(), // pro správu oprávnění uživatele a skupiny - detail oprávnění, např. UsrUserVO a UsrGroupVO
    usersPermissionsByFund: SimpleListReducer(), // pro správu přiřazených k AS, seznam uživatelů
    groupsPermissionsByFund: SimpleListReducer(), // pro správu přiřazených k AS, seznam skupin
};

export default function adminRegion(state = initialState, action = {}) {
    if (action.area && typeof action.area === 'string' && action.area.startsWith('adminRegion.')) {
        // area pro zpracování na předaný fund, ten zde můžeme zpracovat
        let newArea = action.area.split('.');
        return processAreaStores(state, {
            ...action,
            area: newArea.slice(1).join('.'),
        });
    } else if (isPermissionAction(action)) {
        return {
            ...state,
            user: action.area === 'USER' ? user(state.user, action) : state.user,
            group: action.area === 'GROUP' ? group(state.group, action) : state.group,
        };
    } else if (isUserAction(action)) {
        return {
            ...state,
            user: user(state.user, action),
        };
    } else if (isGroupAction(action)) {
        return {
            ...state,
            group: group(state.group, action),
        };
    }

    switch (action.type) {
        case types.LOGIN_SUCCESS: {
            if (action.reset) {
                return initialState;
            }
            return state;
        }
        case types.STORE_LOAD: {
            console.log('ADMIN region LOAD', action.adminRegion);
            if (action.adminRegion) {
                return {
                    ...state,
                    ...action.adminRegion,
                    packages: packages(),
                    fulltext: fulltext(),
                    user: user(action.adminRegion.user, action),
                    group: group(action.adminRegion.group, action),
                };
            }
            return state;
        }
        case types.STORE_SAVE: {
            // const {activeIndex, nodeSettings, extendedView} = state;
            return {
                user: user(state.user, action),
                group: group(state.group, action),
            };
        }
        case types.ADMIN_PACKAGES_REQUEST:
        case types.ADMIN_PACKAGES_RECEIVE:
        case types.ADMIN_PACKAGES_DELETE_RECEIVE:
        case types.ADMIN_PACKAGES_IMPORT_RECEIVE: {
            return {
                ...state,
                packages: packages(state.packages, action),
            };
        }
        case types.ADMIN_FULLTEXT_REINDEXING_REQUEST:
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST:
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_RECEIVE: {
            return {
                ...state,
                fulltext: fulltext(state.fulltext, action),
            };
        }
        case types.CHANGE_INDEXING_FINISHED: {
            const fulltextChange = fulltext(state.fulltext, action);

            if (fulltextChange !== state.fulltext) {
                return {
                    ...state,
                    fulltext: fulltextChange,
                };
            }

            return state;
        }
        case types.CHANGE_PACKAGE: {
            const packagesChange = packages(state.packages, action);

            if (packagesChange !== state.packages) {
                return {
                    ...state,
                    packages: packagesChange,
                };
            }

            return state;
        }
        case types.CHANGE_USER: {
            const userStore = user(state.user, action);

            if (userStore !== state.user) {
                return {
                    ...state,
                    user: userStore,
                };
            }

            return state;
        }
        case types.GROUP_DELETE:
        case types.CHANGE_GROUP: {
            const groupStore = group(state.group, action);

            if (groupStore !== state.group) {
                return {
                    ...state,
                    group: groupStore,
                };
            }

            return state;
        }
        default:
            return state;
    }
}
