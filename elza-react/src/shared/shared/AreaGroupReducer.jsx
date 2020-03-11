import processAreaStores from './../utils/processAreaStores';

const initialState = {
    reducer: areaGroupReducer,
};

const __AGR_INIT = "___AGR_INIT";

/**
 * Reducer pro použití jako skupina standardních (např. list nebo detail) reducerů.
 */
export default function areaGroupReducer(state, action = {type: __AGR_INIT}) {
    if (action.area) {
        return processAreaStores(state, action);
    }

    switch (action.type) {
        case __AGR_INIT: {
            return {
                ...initialState,
                ...state
            }
        }
        default:
            return state;
    }
}

