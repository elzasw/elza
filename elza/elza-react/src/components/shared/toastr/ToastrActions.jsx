/**
 * akce pro toastry
 */

import * as types from 'actions/constants/ActionTypes.js';

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

export function addToastrDanger(title, message = null, size = 'lg') {
    return addToastr(title, message, 'danger', size, null);
}
export function addToastrWarning(title, message = null, size = 'lg') {
    return addToastr(title, message, 'warning', size, null);
}
export function addToastrInfo(title, message = null, size = 'lg') {
    return addToastr(title, message, 'info', size, 2000);
}
export function addToastrSuccess(title, message = null, size = 'lg') {
    return addToastr(title, message, 'success', size, 2000);
}

export function removeToastr(index) {
    return {
        type: types.TOASTR_REMOVE,
        index,
    };
}
