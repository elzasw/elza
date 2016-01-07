import * as types from 'actions/constants/actionTypes';

import ruleSet from './ruleSet'

const initialState = {
    ruleSet: ruleSet(undefined, {type:''})
}

export default function refTables(state = initialState, action) {
    switch (action.type) {
        case types.REF_RULE_SET_REQUEST:
        case types.REF_RULE_SET_RECEIVE:
            return {
                ...state,
                ruleSet: ruleSet(state.ruleSet, action),
            }
            default:
            return state
    }
}


