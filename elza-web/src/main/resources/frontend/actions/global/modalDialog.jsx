/**
 * Akce pro zobrazení a skrytí modálního dialogu.
 */

import * as types from 'actions/constants/ActionTypes';

export function modalDialogShow(component, title, content, dialogClassName='') {
    return {
        type: types.GLOBAL_MODAL_DIALOG_SHOW,
        component,
        title,
        content,
        dialogClassName,
    }
}
export function modalDialogHide() {
    return {
        type: types.GLOBAL_MODAL_DIALOG_HIDE,
    }
}

