/**
 * Akce pro správu skupin uživatelů.
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';

export function isGroupAction(action) {
    switch (action.type) {
        case types.GROUPS_RECEIVE:
        case types.GROUPS_REQUEST:
        case types.GROUPS_SEARCH:
            return true
        default:
            return false
    }
}

function _groupDataKey(group) {
    return group.filterText + '_'
}

export function groupsSearch(filterText) {
    return {
        type: types.GROUPS_SEARCH,
        filterText,
    }
}

/**
 * Fetch dat pro seznam uživatelů.
 */
export function groupsFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const group = state.adminRegion.group;
        const dataKey = _groupDataKey(group)

        if (group.currentDataKey !== dataKey) {
            dispatch(groupsRequest(dataKey))

            WebApi.findGroup(group.filterText)
                .then(json => {
                    var newState = getState();
                    const newGroup = newState.adminRegion.group;
                    const newDataKey = _groupDataKey(newGroup)
                    if (newDataKey === dataKey) {
                        dispatch(groupsReceive(json))
                    }
                })
        }
    }
}

function groupsRequest(dataKey) {
    return {
        type: types.GROUPS_REQUEST,
        dataKey,
    }
}

function groupsReceive(data) {
    return {
        type: types.GROUPS_RECEIVE,
        data,
    }
}
