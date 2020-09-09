import template from './template';
import * as types from 'actions/constants/ActionTypes';

const initialState = {
    items: {},
};

export default function templates(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_TEMPLATES_REQUEST: {
            const {code} = action;
            const initTemplate = state.items.hasOwnProperty(code) ? state.items[code] : template();
            return {
                items: {
                    ...state.items,
                    [code]: template(initTemplate, action),
                },
            };
        }
        case types.REF_TEMPLATES_RECEIVE: {
            const {code} = action;
            if (!state.items.hasOwnProperty(code)) {
                return state;
            }
            return {
                items: {
                    ...state.items,
                    [code]: template(state.items[code], action),
                },
            };
        }
        case types.CHANGE_PACKAGE:
            return initialState;
        default:
            return state;
    }
}
