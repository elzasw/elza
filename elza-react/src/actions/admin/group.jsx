/**
 * Akce pro správu skupin uživatelů.
 */

import * as types from 'actions/constants/ActionTypes';
import {WebApi} from 'actions/index.jsx';
import {i18n} from 'components/shared';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';

export function joinUsers(groupId, userIds) {
    return (dispatch, getState) => {
        WebApi.joinGroup([groupId], userIds).then(() => {
            dispatch(modalDialogHide());
        });
    };
}

export function leaveUser(groupId, userId) {
    return (dispatch, getState) => {
        WebApi.leaveGroup(groupId, userId);
    };
}

export function isGroupAction(action) {
    if (isGroupDetailAction(action)) {
        return true;
    }

    switch (action.type) {
        case types.GROUPS_RECEIVE:
        case types.GROUPS_REQUEST:
        case types.GROUPS_SEARCH:
            return true;
        default:
            return false;
    }
}

export function isGroupDetailAction(action) {
    switch (action.type) {
        case types.GROUPS_SELECT_GROUP:
        case types.GROUPS_GROUP_DETAIL_REQUEST:
        case types.GROUPS_GROUP_DETAIL_RECEIVE:
            return true;
        default:
            return false;
    }
}

function _groupDataKey(group) {
    return group.filterText + '_';
}

function _groupDetailDataKey(groupDetail) {
    if (groupDetail.id !== null) {
        return groupDetail.id + '_';
    } else {
        return '';
    }
}

export function groupsSelectGroup(id) {
    return {
        type: types.GROUPS_SELECT_GROUP,
        id,
    };
}

export function groupsSearch(filterText) {
    return {
        type: types.GROUPS_SEARCH,
        filterText,
    };
}

/**
 * Fetch dat pro detail skupiny.
 */
export function groupsGroupDetailFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const groupDetail = state.adminRegion.group.groupDetail;
        const dataKey = _groupDetailDataKey(groupDetail);

        if (groupDetail.currentDataKey !== dataKey) {
            dispatch(groupsGroupDetailRequest(dataKey));
            WebApi.getGroup(groupDetail.id).then(json => {
                var newState = getState();
                const newGroupDetail = newState.adminRegion.group.groupDetail;
                const newDataKey = _groupDetailDataKey(newGroupDetail);
                if (newDataKey === dataKey) {
                    dispatch(groupsGroupDetailReceive(json));
                }
            });
        }
    };
}

/**
 * Fetch dat pro seznam uživatelů.
 */
export function groupsFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const group = state.adminRegion.group;
        const dataKey = _groupDataKey(group);

        if (group.currentDataKey !== dataKey) {
            dispatch(groupsRequest(dataKey));

            WebApi.findGroup(group.filterText).then(json => {
                var newState = getState();
                const newGroup = newState.adminRegion.group;
                const newDataKey = _groupDataKey(newGroup);
                if (newDataKey === dataKey) {
                    dispatch(groupsReceive(json));
                }
            });
        }
    };
}

function groupsRequest(dataKey) {
    return {
        type: types.GROUPS_REQUEST,
        dataKey,
    };
}

function groupsReceive(data) {
    return {
        type: types.GROUPS_RECEIVE,
        data,
    };
}

function groupsGroupDetailRequest(dataKey) {
    return {
        type: types.GROUPS_GROUP_DETAIL_REQUEST,
        dataKey,
    };
}

function groupsGroupDetailReceive(data) {
    return (dispatch, getState) => {
        // Detail
        dispatch({
            type: types.GROUPS_GROUP_DETAIL_RECEIVE,
            data,
        });

        // Oprávnění z detailu
        // dispatch(permissionReceive("GROUP", data.permissions));
    };
}

export function groupUpdate(groupId, name, description) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateGroup(groupId, name, description));
    };
}

export function groupCreate(name, code, description) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.createGroup(name, code, description)).then(response => {
            dispatch(addToastrSuccess(i18n('admin.group.add.success')));
            dispatch(groupsSelectGroup(response.id));
        });
    };
}

export function groupDelete(id) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.deleteGroup(id));
    };
}
