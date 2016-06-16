/**
 * Akce pro správu uživatelů.
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';

export function isUserAction(action) {
    switch (action.type) {
        case types.USERS_RECEIVE:
        case types.USERS_REQUEST:
        case types.USERS_SEARCH:
            return true
        default:
            return false
    }
}

function _userDataKey(user) {
    return user.filterText + '_' + JSON.stringify(user.filterState)
}

export function usersSearch(filterText, filterState) {
    return {
        type: types.USERS_SEARCH,
        filterText,
        filterState,
    }
}

/**
 * Fetch dat pro seznam uživatelů.
 */
export function usersFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const user = state.adminRegion.user;
        const dataKey = _userDataKey(user)

        if (user.currentDataKey !== dataKey) {
            dispatch(usersRequest(dataKey))

            var active = true
            var disabled = (user.filterState.type === "all")

            WebApi.findUser(user.filterText, active, disabled)
                .then(json => {
                    var newState = getState();
                    const newUser = newState.adminRegion.user;
                    const newDataKey = _userDataKey(newUser)
                    if (newDataKey === dataKey) {
                        dispatch(usersReceive(json))
                    }
                })
        }
    }
}

function usersRequest(dataKey) {
    return {
        type: types.USERS_REQUEST,
        dataKey,
    }
}

function usersReceive(data) {
    return {
        type: types.USERS_RECEIVE,
        data,
    }
}
