import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils';

const initialState = {
    dirty: false,
    isFetching: false,
    fetched: false,
    items: [],
};

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
                items: action.items,
            };

        case types.CHANGE_VISIBLE_POLICY:
            return {
                ...state,
                dirty: true,
            };

        case types.CHANGE_CONFORMITY_INFO:
            var update = false;

            for (var i = 0; i < action.nodeIds; i++) {
                if (indexById(state.items, action.nodeIds[i]) != null) {
                    update = true;
                    break;
                }
            }

            if (update) {
                return {
                    ...state,
                    dirty: true,
                };
            }

            return state;

        default:
            return state;
    }
}
