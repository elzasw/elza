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
    lastKey: 0,
    toasts: [],
};

export default function toastr(state = initialState, action = {}) {
    switch (action.type) {
        case types.TOASTR_ADD:
            var key = state.toasts.length === 0 ? 0 : state.lastKey;
            return {
                lastKey: state.lastKey + 1,
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
                        key: key + 1,
                        visible: true,
                    },
                ],
            };
        case types.TOASTR_REMOVE:
            const index = state.toasts.findIndex(({ key }) => action.key === key);
            return {
                ...state,
                toasts: [...state.toasts.splice(0, index), ...state.toasts.splice(index + 1)],
            };
        default:
            return state;
    }
}
