/**
 * akce pro registry region
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n, AddRegistryForm} from 'components/index.jsx';
import {registryChangeParent, registryRegionDataSelectRecord, registryUnsetParents} from 'actions/registry/registryRegionData.jsx'
import {savingApiWrapper} from 'actions/global/status.jsx';
export function fetchRegistryIfNeeded(search = '', registryParent = null, registerTypeIds = null, versionId = null) {
    return (dispatch, getState) => {
        const {registryRegion} = getState();

        if ((registryRegion.dirty && !registryRegion.isFetching) || (!registryRegion.fetched && !registryRegion.isFetching)) {
            return dispatch(fetchRegistry(search, registryParent, registerTypeIds, versionId));
        }
    }
}

export function fetchRegistry(search, registryParentId = null, registerTypesId = null, versionId = null) {
    return (dispatch) => {
        dispatch(registryListRequest());
        return WebApi.findRegistry(search, registryParentId, registerTypesId, versionId)
            .then(json => {
                dispatch(registryListReceive(search, registryParentId, json));
            });
    }
}

export function registryListReceive(search, registryParentId, json) {
    return {
        type: types.REGISTRY_LIST_RECEIVE,
        records: json.recordList,
        search: search,
        registryParentId: registryParentId,
        countRecords: json.count,
        receivedAt: Date.now()
    }
}

export function registryListRequest() {
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
        if(registryId == null){
            return;
        }
        const {registryRegion: {registryRegionData: {isFetching, fetched, currentDataKey}}} = getState();

        if (currentDataKey !== registryId || (!isFetching && !fetched)) {
            dispatch(getRegistry(registryId));
        }
    }
}

export function getRegistry(registryId) {

    return dispatch => {
        dispatch(registryRecordDetailRequest(registryId));
        if (registryId !== null) {
            return WebApi.getRegistry(registryId)
                .then((json) => dispatch(registryRecordDetailReceive(json)))
                .catch((error) => dispatch(registrySelect()));
        }
    }
}

export function registryRecordDetailRequest(dataKey) {
    return {
        type: types.REGISTRY_RECORD_DETAIL_REQUEST,
        dataKey
    }
}

export function registryRecordDetailReceive(json) {
    return {
        item: json,
        selectedId: json.id,
        type: types.REGISTRY_RECORD_DETAIL_RECEIVE,
        receivedAt: Date.now()
    }
}

export function registryRecordDetailClear() {
    return {
        type: types.REGISTRY_RECORD_DETAIL_CLEAR,
    }
}

export function getRegistryRecordTypesIfNeeded(requestedRegistryTypeId = null) {
    return (dispatch, getState) => {
        const {registryRegionRecordTypes: {fetched, isFetching, registryTypeId}} = getState();
        if ((!fetched && !isFetching) || registryTypeId !== requestedRegistryTypeId) {
            return dispatch(getRegistryRecordTypes(requestedRegistryTypeId));
        }
    }
}

export function getRegistryRecordTypes(registryTypeId = null) {
    return dispatch => {
        dispatch(registryRecordTypesRequest(registryTypeId));
        return WebApi.getRecordTypesForAdd(registryTypeId)
            .then(json => {
                dispatch(registryRecordTypesReceive(json, registryTypeId))
            });
    }
}

export function registryRecordTypesRequest(registryTypeId) {
    return {
        type: types.REGISTRY_RECORD_TYPES_REQUEST,
        registryTypeId
    }
}

export function registryRecordTypesReceive(json, registryTypeId) {
    return {
        item: json,
        registryTypeId: registryTypeId,
        type: types.REGISTRY_RECORD_TYPES_RECEIVE,
        receivedAt: Date.now()
    }
}

export function registryAdd(parentId, versionId, callback, parentName = '', showSubmitTypes = false) {
    return (dispatch) => {
        const title = parentId && parentName ? i18n('registry.addRegistryFor', parentName.substr(0, 40)) : i18n('registry.addRegistry');
        dispatch(modalDialogShow(this, title,
            <AddRegistryForm
                versionId={versionId}
                showSubmitTypes={showSubmitTypes}
                onSubmitForm={(data, submitType) => (dispatch(registryRecordCreate(parentId, callback, data, submitType)))}
                parentRecordId={parentId}
            />
            )
        )

    }
}

function registryRecordCreate(parentId, callback, data, submitType) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.createRecord(data.record, data.characteristics, data.registerTypeId, parentId, data.scopeId)).then(json => {
            dispatch(modalDialogHide());
            callback && callback(json, submitType);
        });
    }
}

export function registrySelect(recordId, fund = null) {
    return (dispatch) => {
        // pokud chceme otevřít složku
        //dispatch(registryClickNavigation(recordId));
        // zrusim vsechny zanoreni v rejstriku
        dispatch(registryUnsetParents(null));
        dispatch(registryRegionDataSelectRecord({
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