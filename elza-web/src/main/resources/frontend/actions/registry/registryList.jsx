/**
 * Web api pro komunikaci se serverem.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function fetchRegistryIfNeeded(search = '', registryParent = null) {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.registry.fetched && !state.registry.isFetching) {
            return dispatch(fetchRegistry(search, registryParent));
        }
    }
}

export function fetchRegistry(search, registryParent = null) {
    return dispatch => {
        dispatch(requestRegistry())

        return WebApi.findRegistry(search, registryParent)
                .then(json => dispatch(receiveRegistry(json)));
    }
}

export function receiveRegistry(json) {
    return {
        type: types.REGISTRY_RECEIVE_REGISTRY_LIST,
        items: json.recordList,
        countItems: json.count,
        receivedAt: Date.now()
    }
}

export function requestRegistry() {
    return {
        type: types.REGISTRY_REQUEST_REGISTRY_LIST
    }
}

export function getRegistryIfNeeded(registryId) {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.registryData.fetched && !state.registryData.isFetching && registryId !==state.registryData.selectedId) {

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



