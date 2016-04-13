import * as types from 'actions/constants/ActionTypes';

import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    data: null
}

export default function visiblePolicy(state = initialState, action = {}) {
    switch (action.type) {

        case types.VISIBLE_POLICY_REQUEST:
            return {
                ...state,
                nodeId: action.nodeId,
                fundVersionId: action.fundVersionId,
                isFetching: true,
            }

        case types.VISIBLE_POLICY_RECEIVE:

            var data = [];
            for (var id in action.policyTypeIds) {
                data.push({id: id, checked: action.policyTypeIds[id]});
            }

            return {
                ...state,
                nodeId: action.nodeId,
                fundVersionId: action.fundVersionId,
                data: data,
                isFetching: false,
                fetched: true,
                dirty: false,
            }

        case types.SET_VISIBLE_POLICY_RECEIVE:
            return {
                ...state,
                dirty: true
            }

        default:
            return state
    }
}

