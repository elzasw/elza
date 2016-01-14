import * as types from 'actions/constants/actionTypes';

import ruleSet from './ruleSet'
import partyNameFormTypes from './partyNameFormTypes'
import partyTypes from './partyTypes'
import recordTypes from './recordTypes'
import rulDataTypes from './rulDataTypes';

const initialState = {
    ruleSet: ruleSet(undefined, {type:''}),
    partyNameFormTypes: partyNameFormTypes(undefined, {type:''}),
    partyTypes: partyTypes(undefined, {type:''}),
    recordTypes: recordTypes(undefined, {type:''}),
    rulDataTypes: rulDataTypes(undefined, {type:''}),
}

export default function refTables(state = initialState, action) {
    switch (action.type) {
        case types.REF_RULE_SET_REQUEST:
        case types.REF_RULE_SET_RECEIVE:
            return {
                ...state,
                ruleSet: ruleSet(state.ruleSet, action),
            }
        case types.REF_PARTY_NAME_FORM_TYPES_REQUEST:
        case types.REF_PARTY_NAME_FORM_TYPES_RECEIVE:
            return {
                ...state,
                partyNameFormTypes: partyNameFormTypes(state.partyNameFormTypes, action),
            }
        case types.REF_PARTY_TYPES_REQUEST:
        case types.REF_PARTY_TYPES_RECEIVE:
            return {
                ...state,
                partyTypes: partyTypes(state.partyTypes, action),
            }
        case types.REF_RECORD_TYPES_REQUEST:
        case types.REF_RECORD_TYPES_RECEIVE:
            return {
                ...state,
                recordTypes: recordTypes(state.recordTypes, action),
            }
        case types.REF_RUL_DATA_TYPES_REQUEST:
        case types.REF_RUL_DATA_TYPES_RECEIVE:
            return {
                ...state,
                rulDataTypes: rulDataTypes(state.rulDataTypes, action),
            }
        default:
            return state
    }
}


