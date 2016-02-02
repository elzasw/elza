import * as types from 'actions/constants/actionTypes';

import ruleSet from './ruleSet'
import partyNameFormTypes from './partyNameFormTypes'
import partyTypes from './partyTypes'
import recordTypes from './recordTypes'
import rulDataTypes from './rulDataTypes';
import calendarTypes from './calendarTypes';
import packetTypes from './packetTypes';
import registryList from './registryList';
import partyList from './partyList';
import scopesData from './scopesData';

const initialState = {
    ruleSet: ruleSet(undefined, {type:''}),
    partyNameFormTypes: partyNameFormTypes(undefined, {type:''}),
    partyTypes: partyTypes(undefined, {type:''}),
    recordTypes: recordTypes(undefined, {type:''}),
    rulDataTypes: rulDataTypes(undefined, {type:''}),
    calendarTypes: calendarTypes(undefined, {type:''}),
    packetTypes: packetTypes(undefined, {type:''}),
    registryList: registryList(undefined, {type:''}),
    partyList: partyList(undefined, {type:''}),
    scopesData: scopesData(undefined, {type:''})
}

export default function refTables(state = initialState, action) {
    switch (action.type) {
        case types.REF_PARTY_LIST_REQUEST:
        case types.REF_PARTY_LIST_RECEIVE:
            return {
                ...state,
                partyList: partyList(state.partyList, action),
            }
        case types.REF_REGISTRY_LIST_REQUEST:
        case types.REF_REGISTRY_LIST_RECEIVE:
            return {
                ...state,
                registryList: registryList(state.registryList, action),
            }
        case types.CHANGE_PACKAGE:
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
        case types.CHANGE_PACKAGE:
        case types.REF_PACKET_TYPES_REQUEST:
        case types.REF_PACKET_TYPES_RECEIVE:
            return {
                ...state,
                packetTypes: packetTypes(state.packetTypes, action),
            }
        case types.REF_SCOPES_TYPES_REQUEST:
        case types.REF_SCOPES_TYPES_RECEIVE:
            return {
                ...state,
                scopesData: scopesData(state.scopesData, action),
            }
        default:
            return state
    }
}


