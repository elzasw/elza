import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    values: {}
}

export default function tab(state = initialState, action = {}) {
    switch (action.type) {
        case types.TAB_SELECT:

            const values = state.values;
            const area = action.area;
            const value = action.value;

            if (area != null) {
                values[area] = value;
            } else {
                console.warn("tab: area is null");
            }

            return {
                ...state,
                values,
            }
        default:
            return state
    }
}
