/**
 * Akce pro zobrazení a skrytí modálního dialogu.
 */

import * as types from 'actions/constants/ActionTypes';

export function modalDialogShow(component, title, content, dialogClassName='', onClose=null) {
    return {
        type: types.GLOBAL_MODAL_DIALOG_SHOW,
        component,
        title,
        content,
        dialogClassName,
        onClose,
    }
}
export function modalDialogHide() {
    return {
        type: types.GLOBAL_MODAL_DIALOG_HIDE,
    }
}

