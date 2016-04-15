import * as types from 'actions/constants/ActionTypes';

const initialState = {
    dirty: false,
    isFetching: false,
    fetched: false,
    items: []
}

export default function fundNodesPolicy(state = initialState, action = {}) {

    switch (action.type) {

        case types.FUND_FUND_NODES_POLICY_REQUEST:
            return {
                ...state,
                isFetching: true,
            };

        case types.FUND_FUND_NODES_POLICY_RECEIVE:
            return {
                ...state,
                fetched: true,
                dirty: false,
                isFetching: false,
                items: action.items
            };

        case types.CHANGE_VISIBLE_POLICY:
        case types.STORE_LOAD:
            return {
                ...state,
                dirty: true,
            }

        /*case types.STORE_LOAD:
            return {
                ...state,
                dirty: true,
            }
        case types.STORE_SAVE:
            return state;*/

        default:
            return state;
    }
}
