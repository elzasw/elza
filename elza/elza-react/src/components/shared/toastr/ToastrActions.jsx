/**
 * akce pro toastry
 */

import * as types from 'actions/constants/ActionTypes';

export function addToastr(title, message = null, style = 'info', size = 'lg', time = null) {
    return {
        type: types.TOASTR_ADD,
        title,
        extended: false,
        message,
        style,
        size,
        time,
        visible: true,
    };
}

export function addToastrExtended(
    title,
    messageComponent,
    messageComponentProps,
    style = 'info',
    size = 'lg',
    time = null,
) {
    return {
        type: types.TOASTR_ADD,
        title,
        extended: true,
        messageComponent,
        messageComponentProps,
        style,
        size,
        time,
        visible: true,
    };
}

export function addToastrDanger(title, message = null, size = 'lg', timeout = null) {
    return addToastr(title, message, 'danger', size, timeout);
}
export function addToastrWarning(title, message = null, size = 'lg', timeout = null) {
    return addToastr(title, message, 'warning', size, timeout);
}
export function addToastrInfo(title, message = null, size = 'lg', timeout = 2000) {
    return addToastr(title, message, 'info', size, timeout);
}
export function addToastrSuccess(title, message = null, size = 'lg', timeout = 2000) {
    return addToastr(title, message, 'success', size, timeout);
}

export function removeToastr(key) {
    return {
        type: types.TOASTR_REMOVE,
        key,
    };
}
