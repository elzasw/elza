/**
 * Web api pro komunikaci se serverem.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {i18n, AddRegistryForm} from 'components';
import {registryChangeParent, registryRegionDataSelectRegistry, registryUnsetParents} from 'actions/registry/registryRegionData'

export function fetchRegistryIfNeeded(search = '', registryParent = null, registerTypeIds = null, versionId = null) {
    return (dispatch, getState) => {
        const {registryRegion} = getState();

        if ((registryRegion.dirty && !registryRegion.isFetching) || (!registryRegion.fetched && !registryRegion.isFetching)) {
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
        type: types.REGISTRY_LIST_RECEIVE,
        records: json.recordList,
        search: search,
        registryParentId: registryParentId,
        countRecords: json.count,
        receivedAt: Date.now()
    }
}

export function requestRegistry() {
    return {
        type: types.REGISTRY_LIST_REQUEST
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
        const {registryRegion} = getState();
        const registryRegionData = getState().registryRegion;

        if (
            (!registryRegionData.isFetching &&
                (registryRegionData.dirty || registryRegionData.requireReload || !registryRegionData.fetched)
            ) || (registryId !== registryRegionData.selectedId)) {
            dispatch(getRegistry(registryId));
        }
    }
}

export function getRegistry(registryId) {

    return dispatch => {
        dispatch(requestRegistryGetRegistry(registryId));
        if (registryId !== null) {
            return WebApi.getRegistry(registryId)
                .then(json => dispatch(receiveRegistryGetRegistry(registryId, json)));
        }
    }
}

export function requestRegistryGetRegistry(registryId) {
    return {
        type: types.REGISTRY_RECORD_DETAIL_REQUEST,
        registryId
    }
}

export function receiveRegistryGetRegistry(registryId, json) {
    return {
        item: json,
        selectedId: registryId,
        type: types.REGISTRY_RECORD_DETAIL_RECEIVE,
        receivedAt: Date.now()
    }
}

export function getRegistryRecordTypesIfNeeded(registryTypeId = null) {
    return (dispatch, getState) => {
        var {registryRegionRecordTypes} = getState();
        if ((!registryRegionRecordTypes.fetched && !registryRegionRecordTypes.isFetching) || registryRegionRecordTypes.registryTypeId !== registryTypeId) {
            return dispatch(getRegistryRecordTypes(registryTypeId));
        }
    }
}

export function getRegistryRecordTypes(registryTypeId = null) {
    return dispatch => {
        dispatch(requestRegistryRecordTypes());
        return WebApi.getRecordTypesForAdd(registryTypeId)
            .then(json => {
                dispatch(receiveRegistryRecordTypes(json, registryTypeId))
            });
    }
}

export function requestRegistryRecordTypes() {
    return {
        type: types.REGISTRY_RECORD_TYPES_REQUEST
    }
}

export function receiveRegistryRecordTypes(json, registryTypeId) {
    return {
        item: json,
        registryTypeId: registryTypeId,
        type: types.REGISTRY_RECORD_TYPES_RECEIVE,
        receivedAt: Date.now()
    }
}

export function registryAdd(parentId, versionId, callback, parentName = '', showSubmitTypes = false) {
    return (dispatch, getState) => {
        var title = i18n('registry.addRegistry');
        if (parentId && parentName) {
            title = i18n('registry.addRegistryFor', parentName.substr(0, 40));
        }
        dispatch(modalDialogShow(this, title,
            <AddRegistryForm
                versionId={versionId}
                showSubmitTypes={showSubmitTypes}
                onSubmitForm={registryAddSubmit.bind(null, parentId, callback, dispatch)}
                parentRecordId={parentId}
            />
            )
        )

    }
}

function registryAddSubmit(parentId, callback, dispatch, data, submitType) {
    WebApi.createRecord(data.record, data.characteristics, data.registerTypeId, parentId, data.scopeId).then(json => {
        dispatch(modalDialogHide());
        callback && callback(json, submitType);
    });
}

export function registrySelect(recordId, fund = null) {
    return (dispatch) => {
        // pokud chceme otevřít složku
        //dispatch(registryClickNavigation(recordId));
        // zrusim vsechny zanoreni v rejstriku
        dispatch(registryUnsetParents(null));
        dispatch(registryRegionDataSelectRegistry({
            selectedId: recordId
        }));
        dispatch(registrySelectDo(recordId, fund));
    }
}

export function registrySelectDo(recordId, fund = null) {
    return {
        recordId: recordId,
        fund,
        type: types.REGISTRY_SELECT
    }
}

export function registryArrReset() {
    return {
        type: types.REGISTRY_ARR_RESET
    }

}

export function registryClickNavigation(recordId) {
    return (dispatch, getState) => {
        var state = getState();
        return WebApi.getRegistry(recordId).then(json => {
                json.parents.push({id: recordId, name: json.record});

                const registry = {
                    ...registry,
                    registryParentId: recordId,
                    parents: json.parents,
                    typesToRoot: json.typesToRoot,
                    filterText: '',
                    registryTypesId: state.registryRegion.registryTypesId
                };
                dispatch(registryChangeParent(registry));
            }
        );
    }
}