import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

// Record types

export function getRegistryRecordTypesIfNeeded(requestedRegistryTypeId = null) {
    return (dispatch, getState) => {
        const {
            registryRegionRecordTypes: {fetched, isFetching, registryTypeId},
        } = getState();
        if ((!fetched && !isFetching) || registryTypeId !== requestedRegistryTypeId) {
            return dispatch(getRegistryRecordTypes(requestedRegistryTypeId));
        }
    };
}

export function getRegistryRecordTypes(registryTypeId = null) {
    return dispatch => {
        dispatch(registryRecordTypesRequest(registryTypeId));
        return WebApi.getRecordTypesForAdd(registryTypeId).then(json => {
            dispatch(registryRecordTypesReceive(json, registryTypeId));
        });
    };
}

export function registryRecordTypesRequest(registryTypeId) {
    return {
        type: types.REGISTRY_RECORD_TYPES_REQUEST,
        registryTypeId,
    };
}

export function registryRecordTypesReceive(json, registryTypeId) {
    return {
        item: json,
        registryTypeId: registryTypeId,
        type: types.REGISTRY_RECORD_TYPES_RECEIVE,
        receivedAt: Date.now(),
    };
}
