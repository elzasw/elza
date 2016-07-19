/**
 * Akce pro správu uživatelů.
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';
import {permissionReceive} from "./permission.jsx"

export function isUserAction(action) {
    if (isUserDetailAction(action)) {
        return true;
    }
    
    switch (action.type) {
        case types.USERS_RECEIVE:
        case types.USERS_REQUEST:
        case types.USERS_SEARCH:
            return true
        default:
            return false
    }
}

export function isUserDetailAction(action) {
    switch (action.type) {
        case types.USERS_SELECT_USER:
        case types.USERS_USER_DETAIL_REQUEST:
        case types.USERS_USER_DETAIL_RECEIVE:
            return true
        default:
            return false
    }
}

function _userDataKey(user) {
    return user.filterText + '_' + JSON.stringify(user.filterState)
}

function _userDetailDataKey(userDetail) {
    if (userDetail.id !== null) {
        return userDetail.id + '_'
    } else {
        return ''
    }
}

export function usersSelectUser(id) {
    return {
        type: types.USERS_SELECT_USER,
        id,
    }
}

export function usersSearch(filterText, filterState) {
    return {
        type: types.USERS_SEARCH,
        filterText,
        filterState,
    }
}

/**
 * Fetch dat pro detail uživatele.
 */
export function usersUserDetailFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const userDetail = state.adminRegion.user.userDetail
        const dataKey = _userDetailDataKey(userDetail)

        if (userDetail.currentDataKey !== dataKey) {
            dispatch(usersUserDetailRequest(dataKey))
            WebApi.getUser(userDetail.id)
                .then(json => {
                    var newState = getState();
                    const newUserDetail = newState.adminRegion.user.userDetail;
                    const newDataKey = _userDetailDataKey(newUserDetail)
                    if (newDataKey === dataKey) {
                        dispatch(usersUserDetailReceive(json))
                    }
                })
        }
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

function usersUserDetailRequest(dataKey) {
    return {
        type: types.USERS_USER_DETAIL_REQUEST,
        dataKey,
    }
}

function usersUserDetailReceive(data) {
    return (dispatch, getState) => {
        // Detail
        dispatch({
            type: types.USERS_USER_DETAIL_RECEIVE,
            data,
        })
        
        // Oprávnění z detailu
        // dispatch(permissionReceive("USER", data.permissions));
    }
}


export function userCreate(username, password, partyId) {
    return WebApi.createUser(username, password, partyId)
}

export function userPasswordChange(oldPass, newPass) {
    return WebApi.changePasswordUser(oldPass, newPass)
}

export function adminPasswordChange(userId, newPassword) {
    return WebApi.changePassword(userId, newPassword)
}
