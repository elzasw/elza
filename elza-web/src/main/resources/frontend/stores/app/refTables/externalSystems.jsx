import * as types from "actions/constants/ActionTypes.js";

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: []
};

export default function externalSystems(state = initialState, action = {}) {
    switch (action.type) {

        case types.REF_EXTERNAL_SYSTEMS_REQUEST: {
            return {
                ...state,
                isFetching: true
            }
        }
        case types.REF_EXTERNAL_SYSTEMS_RECEIVE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: Date.now()
            }
        }
        case types.REF_EXTERNAL_SYSTEMS_INVALID: {
            return {
                dirty: true
            }
        }

        default:
            return state
    }
}
