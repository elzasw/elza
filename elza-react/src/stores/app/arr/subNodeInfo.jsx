import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    isFetching: false,
    fetched: false,
    childNodes: [],
};

export default function subNodeInfo(state = initialState, action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_INFO_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            });
        case types.FUND_SUB_NODE_INFO_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                childNodes: action.childNodes,
            });
        default:
            return state;
    }
}
