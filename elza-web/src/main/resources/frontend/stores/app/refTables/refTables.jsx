import * as types from 'actions/constants/actionTypes';

import ruleSet from './ruleSet'
import nameFormType from './nameFormType'

const initialState = {
    ruleSet: ruleSet(undefined, {type:''}),
    nameFormType: nameFormType(undefined, {type:''})
}

export default function refTables(state = initialState, action) {
    switch (action.type) {
        case types.REF_RULE_SET_REQUEST:
        case types.REF_RULE_SET_RECEIVE:
            return {
                ...state,
                ruleSet: ruleSet(state.ruleSet, action),
            }
        case types.REF_NAME_FORM_TYPE_REQUEST:
        case types.REF_NAME_FORM_TYPE_RECEIVE:
            return {
                ...state,
                nameFormType: nameFormType(state.nameFormType, action),
            }
        default:
        return state
    }
}


