/**
 * Web api pro komunikaci se serverem.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {i18n, AddRegistryForm} from 'components';
import {registryChangeParent, registryRegionData, registryUnsetParents} from 'actions/registry/registryRegionData'

export function fetchRegistryIfNeeded(search = '', registryParent = null, registerTypeIds = null, versionId = null) {
    return (dispatch, getState) => {
        var state = getState();

        if ((state.registryRegion.dirty && !state.registryRegion.isFetching) || (!state.registryRegion.fetched && !state.registryRegion.isFetching)) {
            return dispatch(fetchRegistry(search, registryParent, registerTypeIds, versionId));
        }
    }
}

export function fetchRegistry(search, registryParentId = null, registerTypesId = null, versionId = null) {
    return dispatch => {
        dispatch(requestRegistry());
        return WebApi.findRegistry(search, registryParentId, registerTypesId, versionId)
                .then(json => {
                    dispatch(receiveRegistry(search, registryParentId, json));
                });
    }
}

export function receiveRegistry(search, registryParentId, json) {
    return {
        type: types.REGISTRY_RECEIVE_REGISTRY_LIST,
        records: json.recordList,
        search: search,
        registryParentId: registryParentId,
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
        if ((!state.registryRegionData.isFetching && state.registryRegionData.dirty) || !state.registryRegionData.fetched && !state.registryRegionData.isFetching && (registryId !==state.registryRegionData.selectedId || state.registryRegionData.requireReload === true)) {
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

export function getRegistryRecordTypesIfNeeded(registryTypeId = null){
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.registryRegionRecordTypes.fetched && !state.registryRegionRecordTypes.isFetching) || state.registryRegionRecordTypes.registryTypeId !== registryTypeId) {
            return dispatch(getRegistryRecordTypes(registryTypeId));
        }
    }
}

export function getRegistryRecordTypes(registryTypeId = null){
    return dispatch => {
        dispatch(requestRegistryRecordTypes())
        return WebApi.getRecordTypesForAdd(registryTypeId)
            .then(json => {dispatch(receiveRegistryRecordTypes(json, registryTypeId))});
    }
}

export function requestRegistryRecordTypes() {
    return {
        type: types.REGISTRY_REQUEST_REGISTRY_RECORD_TYPES
    }
}

export function receiveRegistryRecordTypes(json, registryTypeId){
    return {
        item: json,
        registryTypeId: registryTypeId,
        type: types.REGISTRY_RECIVE_REGISTRY_RECORD_TYPES,
        receivedAt: Date.now()
    }
}

export function registryAdd(parentId, versionId, callback, parentName = '') {
    return (dispatch, getState) => {
        var state = getState();
        var nadpis = i18n('registry.addRegistry');
        if (parentId && parentName){
            var shortenParent = parentName.substr(0,40);
            nadpis = i18n('registry.addRegistry.for')+' '+shortenParent
        }
        dispatch(modalDialogShow(this, nadpis,
                        <AddRegistryForm
                                create
                                versionId={versionId}
                                onSubmitForm={registryAddSubmit.bind(null, parentId, callback, dispatch)}
                                parentRecordId={parentId}
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

export function registrySelect(recordId, fa = null) {
    return (dispatch) => {
        // pokud chceme otevřít složku
        //dispatch(registryClickNavigation(recordId));
        // zrusim vsechny zanoreni v rejstriku
        dispatch(registryUnsetParents(null));
        var registry = Object.assign({}, registry,{selectedId: recordId});
        dispatch(registryRegionData(registry));
        dispatch(registrySelectDo(recordId, fa));
    }
}

export function registrySelectDo(recordId, fa = null){
    return {
        recordId: recordId,
        fa: fa,
        type: types.REGISTRY_SELECT
    }
}

export function registryArrReset() {
    return {
        type: types.REGISTRY_ARR_RESET
    }

}

export function registryClickNavigation(recordId){
    return (dispatch, getState) => {
        var state = getState();
        return WebApi.getRegistry(recordId).then(json => {
            json.parents.push({id:recordId, name: json.record});

            var registry = Object.assign({}, registry,{registryParentId: recordId, parents: json.parents, typesToRoot: json.typesToRoot, filterText: '', registryTypesId: state.registryRegion.registryTypesId});
            dispatch(registryChangeParent(registry));
        });
    }
}