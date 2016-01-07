import * as types from 'actions/constants/actionTypes';

const initialState = {
    visible: false,
    component: null,
    title: null,
    content: null,
}

export default function modalDialog(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_MODAL_DIALOG_SHOW:
            return Object.assign({}, state, {
                visible: true,
                title: action.title,
                component: action.component,
                content: action.content,
            })
        case types.GLOBAL_MODAL_DIALOG_HIDE:
            return Object.assign({}, state, {
                visible: false,
            })
        default:
            return state
    }
}
