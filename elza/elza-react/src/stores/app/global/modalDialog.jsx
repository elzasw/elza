import * as types from 'actions/constants/ActionTypes';

const initialState = {
    items: [],
    lastKey: undefined,
};

export default function modalDialog(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_MODAL_DIALOG_SHOW:
            const key = !state.lastKey ? 1 : state.lastKey + 1;
            return {
                lastKey: key,
                items: [
                    ...state.items,
                    {
                        title: action.title,
                        component: action.component,
                        content: action.content,
                        dialogClassName: action.dialogClassName,
                        onClose: action.onClose,
                        key: key,
                    },
                ],
            };
        case types.GLOBAL_MODAL_DIALOG_HIDE:
            const hideIndex = state.items.findIndex(({ key }) => action.key != undefined ? key === action.key : key === state.lastKey);
            const items = [...state.items];
            items.splice(hideIndex, 1);
            // const items = state.items.length > 1 ? [...state.items.slice(0, hideIndex)] : [];

            return {
                ...state,
                lastKey: items.length === 0 ? undefined : state.lastKey,
                items
            }
        default:
            return state;
    }
}
