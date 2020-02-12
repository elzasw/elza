import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    values: {}
};

/**
 * Tab store slouží jako mapa otevřených tabů
 *
 * {[area]: "OTEVRENY_KEY"}
 *
 * @param state
 * @param action
 * @returns {*}
 */
export default function tab(state = initialState, action = {}) {
    switch (action.type) {
        case types.TAB_SELECT:
            const {area, value} = action;
            if (!area) {
                console.warn("tab: area is null");
                return state;
            }

            return {
                ...state,
                values: {
                    ...state.values,
                    [area]: value
                },
            };
        default:
            return state
    }
}
