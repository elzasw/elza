import * as types from 'actions/constants/actionTypes';

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

