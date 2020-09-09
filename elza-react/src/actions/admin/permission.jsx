/**
 * Akce pro správu oprávnění u skupiny a uživatele.
 */

import * as types from 'actions/constants/ActionTypes';
import {WebApi} from 'actions/index.jsx';

export function changeUserPermission(userId, permissions) {
    return (dispatch, getState) => {
        WebApi.changeUserPermission(userId, permissions);
    };
}
export function changeGroupPermission(groupId, permissions) {
    return (dispatch, getState) => {
        WebApi.changeGroupPermission(groupId, permissions);
    };
}

export function isPermissionAction(action) {
    switch (action.type) {
        case types.PERMISSIONS_PERMISSION_CHANGE:
        case types.PERMISSIONS_PERMISSION_ADD:
        case types.PERMISSIONS_PERMISSION_REMOVE:
        case types.PERMISSIONS_PERMISSION_RECEIVE:
            return true;
        default:
            return false;
    }
}

export function permissionAdd(area) {
    return {
        type: types.PERMISSIONS_PERMISSION_ADD,
        area,
    };
}
export function permissionRemove(area, index) {
    return {
        type: types.PERMISSIONS_PERMISSION_REMOVE,
        area,
        index,
    };
}
export function permissionChange(area, index, value) {
    return {
        type: types.PERMISSIONS_PERMISSION_CHANGE,
        area,
        index,
        value,
    };
}

export function permissionReceive(area, permissions) {
    return {
        type: types.PERMISSIONS_PERMISSION_RECEIVE,
        area,
        permissions,
    };
}
export function permissionBlur(area, index) {
    return (dispatch, getState) => {};
}
