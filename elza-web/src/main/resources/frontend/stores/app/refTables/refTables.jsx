import * as types from 'actions/constants/actionTypes';

import ruleSet from './ruleSet'
import partyNameFormTypes from './partyNameFormTypes'
import partyTypes from './partyTypes'
import recordTypes from './recordTypes'
import rulDataTypes from './rulDataTypes';
import calendarTypes from './calendarTypes';
import packetTypes from './packetTypes';
import relationTypes from './relationTypes';
import relationRoleTypes from './relationRoleTypes';

const initialState = {
    ruleSet: ruleSet(undefined, {type:''}),
    partyNameFormTypes: partyNameFormTypes(undefined, {type:''}),
    partyTypes: partyTypes(undefined, {type:''}),
    recordTypes: recordTypes(undefined, {type:''}),
    rulDataTypes: rulDataTypes(undefined, {type:''}),
    calendarTypes: calendarTypes(undefined, {type:''}),
    packetTypes: packetTypes(undefined, {type:''}),
    relationTypes: relationTypes(undefined, {type:''}),
    relationRoleTypes: relationRoleTypes(undefined, {type:''}),
}

export default function refTables(state = initialState, action) {
    switch (action.type) {
        case types.REF_RELATION_TYPES_REQUEST:
        case types.REF_RELATION_TYPES_RECEIVE:
            return {
                ...state,
                relationTypes: relationTypes(state.relationTypes, action),
            }
        case types.REF_RELATION_ROLE_TYPES_REQUEST:
        case types.REF_RELATION_ROLE_TYPES_RECEIVE:
            return {
                ...state,
                relationRoleTypes: relationRoleTypes(state.relationRoleTypes, action),
            }
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
        case types.REF_CALENDAR_TYPES_REQUEST:
        case types.REF_CALENDAR_TYPES_RECEIVE:
            return {
                ...state,
                calendarTypes: calendarTypes(state.calendarTypes, action),
            }
        case types.REF_PACKET_TYPES_REQUEST:
        case types.REF_PACKET_TYPES_RECEIVE:
            return {
                ...state,
                packetTypes: packetTypes(state.packetTypes, action),
            }
        default:
            return state
    }
}


