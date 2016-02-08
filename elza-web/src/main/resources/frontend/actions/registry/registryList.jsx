/**
 * Web api pro komunikaci se serverem.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {i18n, AddRegistryForm} from 'components';

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

export function getRegistryRecordTypesIfNeeded(partyTypeId = null){
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.registryRecordTypes.fetched && !state.registryRecordTypes.isFetching) || state.registryRecordTypes.partyTypeId !== partyTypeId) {
            return dispatch(getRegistryRecordTypes(partyTypeId));
        }
    }
}

export function getRegistryRecordTypes(partyTypeId = null){
    return dispatch => {
        dispatch(requestRegistryRecordTypes())
        return WebApi.getRecordTypesForAdd(partyTypeId)
            .then(json => {dispatch(receiveRegistryRecordTypes(json, partyTypeId))});
    }
}

export function requestRegistryRecordTypes() {
    return {
        type: types.REGISTRY_REQUEST_REGISTRY_RECORD_TYPES
    }
}

export function receiveRegistryRecordTypes(json, partyTypeId){
    return {
        item: json,
        partyTypeId: partyTypeId,
        type: types.REGISTRY_RECIVE_REGISTRY_RECORD_TYPES,
        receivedAt: Date.now()
    }
}

export function registryAdd(parentId, callback) {
    return (dispatch, getState) => {
        var state = getState();
        var registryParentTypesId = null;

        if (state.registry.registryData) {
            registryParentTypesId = state.registry.registryData.item.registerTypeId;
        } else if(state.registryData.item.registerTypeId) {
            registryParentTypesId = state.registryData.item.registerTypeId;
        }

        dispatch(modalDialogShow(this, i18n('registry.addRegistry'),
                        <AddRegistryForm
                                create
                                onSubmit={registryAddSubmit.bind(null, parentId, callback, dispatch)}
                                parentRecordId={parentId}
                                parentRegisterTypeId={registryParentTypesId}
                                />
                )
        )

    }
}

function registryAddSubmit(parentId, callback, dispatch, data) {
    WebApi.insertRegistry( data.nameMain, data.characteristics, data.registerTypeId, parentId, data.scopeId ).then(json => {
        dispatch(modalDialogHide());
        callback && callback(json);
    });
}

export function registrySelect(recordId) {
    return {
        recordId: recordId,
        type: types.REGISTRY_SELECT,
        receivedAt: Date.now()
    }

}