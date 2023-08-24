/**
 * Store pro toastr
 *
 * @author Petr Compel
 * @since 15.2.2015
 */
import * as types from 'actions/constants/ActionTypes';

/**
 * Výchozí stav store
 */
const initialState = {
    lastKey: undefined,
    toasts: [],
};

export default function toastr(state = initialState, action = {}) {
    switch (action.type) {
        case types.TOASTR_ADD:
            const key = !state.lastKey ? 1 : state.lastKey + 1;
            return {
                lastKey: key,
                toasts: [
                    ...state.toasts,
                    {
                        style: action.style,
                        title: action.title,
                        extended: action.extended,
                        message: action.message,
                        messageComponent: action.messageComponent,
                        messageComponentProps: action.messageComponentProps,
                        size: action.size,
                        time: action.time,
                        key: key,
                        visible: true,
                    },
                ],
            };
        case types.TOASTR_REMOVE:
            const index = state.toasts.findIndex(({ key }) => action.key === key);
            const toasts = [...state.toasts.splice(0, index), ...state.toasts.splice(index + 1)];
            return {
                ...state,
                lastKey: toasts.length === 0 ? undefined : state.lastKey,
                toasts,
            };
        default:
            return state;
    }
}
