/**
 * Akce pro zobrazení a skrytí modálního dialogu.
 */

import * as types from 'actions/constants/ActionTypes';

export function modalDialogShow(component, title, content) {
    return {
        type: types.GLOBAL_MODAL_DIALOG_SHOW,
        component,
        title,
        content,
    }
}
export function modalDialogHide() {
    return {
        type: types.GLOBAL_MODAL_DIALOG_HIDE,
    }
}

