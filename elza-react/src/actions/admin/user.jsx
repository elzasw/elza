/**
 * Akce pro správu uživatelů.
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import i18n from '../../components/i18n';

export function joinGroups(userId, groupIds) {
    return (dispatch, getState) => {
        WebApi.joinGroup(groupIds, [userId])
              .then(() => {
                  dispatch(modalDialogHide());
              });
    };
}

export function leaveGroup(userId, groupId) {
    return (dispatch, getState) => {
        WebApi.leaveGroup(groupId, userId);
    };
}

export function isUserAction(action) {
    if (isUserDetailAction(action)) {
        return true;
    }

    switch (action.type) {
        case types.USERS_RECEIVE:
        case types.USERS_REQUEST:
        case types.USERS_SEARCH:
            return true;
        default:
            return false;
    }
}

export function isUserDetailAction(action) {
    switch (action.type) {
        case types.USERS_SELECT_USER:
        case types.USERS_USER_DETAIL_REQUEST:
        case types.USERS_USER_DETAIL_RECEIVE:
            return true;
        default:
            return false;
    }
}

function _userDataKey(user) {
    return user.filterText + '_' + JSON.stringify(user.filterState);
}

function _userDetailDataKey(userDetail) {
    if (userDetail.id !== null) {
        return userDetail.id + '_';
    } else {
        return '';
    }
}

export function usersSelectUser(id) {
    return {
        type: types.USERS_SELECT_USER,
        id,
    };
}

export function usersSearch(filterText, filterState) {
    return {
        type: types.USERS_SEARCH,
        filterText,
        filterState,
    };
}

/**
 * Fetch dat pro detail uživatele.
 * @param force - pokud bude true, provede se fetch vždy
 */
export function usersUserDetailFetchIfNeeded(force = false) {
    return (dispatch, getState) => {
        var state = getState();
        const userDetail = state.adminRegion.user.userDetail;
        const dataKey = _userDetailDataKey(userDetail);

        if (force || userDetail.currentDataKey !== dataKey) {
            dispatch(usersUserDetailRequest(dataKey));
            WebApi.getUser(userDetail.id)
                  .then(json => {
                      var newState = getState();
                      const newUserDetail = newState.adminRegion.user.userDetail;
                      const newDataKey = _userDetailDataKey(newUserDetail);
                      if (newDataKey === dataKey) {
                          dispatch(usersUserDetailReceive(json));
                      }
                  });
        }
    };
}

/**
 * Fetch dat pro seznam uživatelů.
 */
export function usersFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const user = state.adminRegion.user;
        const dataKey = _userDataKey(user);

        if (user.currentDataKey !== dataKey) {
            dispatch(usersRequest(dataKey));

            var active = true;
            var disabled = (user.filterState.type === 'all');

            WebApi.findUser(user.filterText, active, disabled)
                  .then(json => {
                      var newState = getState();
                      const newUser = newState.adminRegion.user;
                      const newDataKey = _userDataKey(newUser);
                      if (newDataKey === dataKey) {
                          dispatch(usersReceive(json));
                      }
                  });
        }
    };
}

function usersRequest(dataKey) {
    return {
        type: types.USERS_REQUEST,
        dataKey,
    };
}

function usersReceive(data) {
    return {
        type: types.USERS_RECEIVE,
        data,
    };
}

function usersUserDetailRequest(dataKey) {
    return {
        type: types.USERS_USER_DETAIL_REQUEST,
        dataKey,
    };
}

function usersUserDetailReceive(data) {
    return (dispatch, getState) => {
        // Detail
        dispatch({
            type: types.USERS_USER_DETAIL_RECEIVE,
            data,
        });

        // Oprávnění z detailu
        // dispatch(permissionReceive("USER", data.permissions));
    };
}


export function userCreate(username, valuesMap, partyId) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.createUser(username, valuesMap, partyId)).then(response => {
            dispatch(addToastrSuccess(i18n('admin.user.add.success')));
            dispatch(usersSelectUser(response.id));
        });
    };
}

export function userUpdate(id, username, valuesMap) {
    return (dispatch) => {
        return savingApiWrapper(dispatch, WebApi.updateUser(id, username, valuesMap)).then(response => {
            dispatch(addToastrSuccess(i18n('admin.user.update.success')));
            dispatch(usersSelectUser(response.id));
        });
    };
}

export function userPasswordChange(oldPass, newPass) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.changePasswordUser(oldPass, newPass)).then(response => {
            dispatch(addToastrSuccess(i18n('admin.user.passwordChange.success')));
        });
    };
}

export function adminPasswordChange(userId, newPassword) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.changePassword(userId, newPassword)).then(response => {
            dispatch(addToastrSuccess(i18n('admin.user.passwordChange.success')));
        });
    };
}

export function adminUserChangeActive(userId, active) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.changeActive(userId, active));
    };
}
