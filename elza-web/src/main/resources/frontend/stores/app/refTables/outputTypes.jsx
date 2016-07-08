import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: []
};

export default function outputTypes(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_OUTPUT_TYPES_REQUEST:{
            return {
                ...state,
                isFetching: true
            }
        }
        case types.REF_OUTPUT_TYPES_RECEIVE:{
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt
            }
        }
        case types.CHANGE_PACKAGE:{
            return {
                dirty: true
            }
        }
        default:
            return state;
    }
}
