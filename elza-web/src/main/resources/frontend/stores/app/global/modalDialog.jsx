import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    items: []
};

export default function modalDialog(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_MODAL_DIALOG_SHOW:
            return {
                items: [
                    {
                        title: action.title,
                        component: action.component,
                        content: action.content,
                        dialogClassName: action.dialogClassName,
                        onClose: action.onClose,
                    },
                    ...state.items
                ]
            };
        case types.GLOBAL_MODAL_DIALOG_HIDE:
            return state.items.length > 0 ? {
                items: [...state.items.slice(1)]
            } : state;
        default:
            return state
    }
}
