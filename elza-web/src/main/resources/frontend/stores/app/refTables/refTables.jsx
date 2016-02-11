import * as types from 'actions/constants/ActionTypes';

import ruleSet from './ruleSet'
import partyNameFormTypes from './partyNameFormTypes'
import partyTypes from './partyTypes'
import recordTypes from './recordTypes'
import rulDataTypes from './rulDataTypes';
import calendarTypes from './calendarTypes';
import packetTypes from './packetTypes';
import registryRegionList from './registryRegionList';
import partyList from './partyList';
import scopesData from './scopesData';
import descItemTypes from './descItemTypes';

const initialState = {
    ruleSet: ruleSet(undefined, {type:''}),
    partyNameFormTypes: partyNameFormTypes(undefined, {type:''}),
    partyTypes: partyTypes(undefined, {type:''}),
    recordTypes: recordTypes(undefined, {type:''}),
    rulDataTypes: rulDataTypes(undefined, {type:''}),
    calendarTypes: calendarTypes(undefined, {type:''}),
    packetTypes: packetTypes(undefined, {type:''}),
    registryRegionList: registryRegionList(undefined, {type:''}),
    partyList: partyList(undefined, {type:''}),
    scopesData: scopesData(undefined, {type:''}),
    descItemTypes: descItemTypes()
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
                registryRegionList: registryRegionList(state.registryRegionList, action),
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
        case types.REF_SCOPES_TYPES_DIRTY:
        case types.REF_SCOPES_TYPES_FETCHING:
        case types.REF_SCOPES_TYPES_REQUEST:
        case types.REF_SCOPES_TYPES_RECEIVE:
            return {
                ...state,
                scopesData: scopesData(state.scopesData, action),
            }

        case types.REF_DESC_ITEM_TYPES_REQUEST:
        case types.REF_DESC_ITEM_TYPES_RECEIVE:
            return {
                ...state,
                descItemTypes: descItemTypes(state.descItemTypes, action),
            }

        case types.CHANGE_PACKAGE:
            return {
                ...state,
                packetTypes: packetTypes(state.packetTypes, action),
                ruleSet: ruleSet(state.ruleSet, action),
                descItemTypes: descItemTypes(state.descItemTypes, action),
            }
        default:
            return state
    }
}


