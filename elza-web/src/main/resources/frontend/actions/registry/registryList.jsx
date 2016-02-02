/**
 * Web api pro komunikaci se serverem.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function fetchRegistryIfNeeded(search = '', registryParent = null, registerTypeIds = null) {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.registry.fetched && !state.registry.isFetching) {
            return dispatch(fetchRegistry(search, registryParent, registerTypeIds));
        }
    }
}

export function fetchRegistry(search, registryParent = null, registerTypeIds = null) {
    return dispatch => {
        dispatch(requestRegistry())

        return WebApi.findRegistry(search, registryParent, registerTypeIds)
                .then(json => dispatch(receiveRegistry(json)));
    }
}

export function receiveRegistry(json) {
    return {
        type: types.REGISTRY_RECEIVE_REGISTRY_LIST,
        records: json.recordList,
        countRecords: json.count,
        receivedAt: Date.now()
    }
}

export function requestRegistry() {
    return {
        type: types.REGISTRY_REQUEST_REGISTRY_LIST
    }
}

export function registrySetTypesId(registryTypesId) {
    return {
        type: types.REGISTRY_CHANGED_TYPES_ID,
        registryTypesId: registryTypesId
    }
}

export function getRegistryIfNeeded(registryId) {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.registryData.fetched && !state.registryData.isFetching && (registryId !==state.registryData.selectedId || state.registryData.requireReload === true)) {
            return dispatch(getRegistry(registryId));
        }
    }
}

export function getRegistry(registryId) {

    return dispatch => {
        dispatch(requestRegistryGetRegistry())
        return WebApi.getRegistry(registryId)
                .then(json => dispatch(receiveRegistryGetRegistry(registryId, json)));
    }
}

export function requestRegistryGetRegistry() {
    return {
        type: types.REGISTRY_REQUEST_REGISTRY_DETAIL
    }
}

export function receiveRegistryGetRegistry(registryId, json) {
    return {
        item: json,
        selectedId: registryId,
        type: types.REGISTRY_RECEIVE_REGISTRY_DETAIL,
        receivedAt: Date.now()
    }
}

export function getRegistryRecordTypesIfNeeded(){
    return (dispatch, getState) => {
        var state = getState();
        if (!state.registryRecordTypes.fetched && !state.registryRecordTypes.isFetching) {
            return dispatch(getRegistryRecordTypes());
        }
    }
}

export function getRegistryRecordTypes(){
    return dispatch => {
        dispatch(requestRegistryRecordTypes())
        return WebApi.getRecordTypesForAdd()
            .then(json => {dispatch(receiveRegistryRecordTypes(json))});
    }
}

export function requestRegistryRecordTypes() {
    return {
        type: types.REGISTRY_REQUEST_REGISTRY_RECORD_TYPES
    }
}

export function receiveRegistryRecordTypes(json){
    return {
        item: json,
        type: types.REGISTRY_RECIVE_REGISTRY_RECORD_TYPES,
        receivedAt: Date.now()
    }
}