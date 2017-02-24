import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'
import {storeFromArea, indexById} from 'shared/utils'

import {DEFAULT_LIST_SIZE, MODAL_DIALOG_VARIANT} from 'constants'
export const DEFAULT_REGISTRY_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;
export const AREA_REGISTRY_LIST = "registryList";
import * as types from 'actions/constants/ActionTypes.js';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {i18n, AddRegistryForm} from 'components/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess,addToastrDanger} from 'components/shared/toastr/ToastrActions.jsx'




/**
 * Načtení seznamu rejstříků dle filtru
 *
 * @param from {number} od kolikáté položky se má posílat seznam - stránkování
 * @param size {number} počet položek v seznamu - velikost jedné stránky
 */
export function registryListFetchIfNeeded(from = 0, size = DEFAULT_REGISTRY_LIST_MAX_SIZE) {
    return SimpleListActions.fetchIfNeeded(AREA_REGISTRY_LIST, true, (parent, filter) => WebApi.findRegistry(filter.text, filter.registryParentId, filter.registryTypeId, filter.versionId, filter.itemSpecId, from, size));
}

/**
 * Filtr osob
 *
 * @param filter {Object} - objekt filtru
 */
export function registryListFilter(filter) {
    return SimpleListActions.filter(AREA_REGISTRY_LIST, filter);
}

/**
 * Invalidace seznamu rejstříků
 */
export function registryListInvalidate() {
    return SimpleListActions.invalidate(AREA_REGISTRY_LIST, null);
}



export function registryMove(registryParentId) {
    return (dispatch, getState) => {
        const store = getState();
        const list = storeFromArea(store, AREA_REGISTRY_LIST);
        if (!list.recordForMove) {
            console.error('Not selected record for move');
            return;
        }

        WebApi.getRegistry(list.recordForMove.id).then((data) => {
            savingApiWrapper(dispatch, WebApi.updateRegistry({...data, registryParentId})).then(json => {
                dispatch(registryMoveFinish());
            });
        });
    }
}


export function registryMoveStart(data) {

    return {
        type: types.REGISTRY_MOVE_REGISTRY_START,
        area: AREA_REGISTRY_LIST,
        data
    }
}

export function registryMoveCancel() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_CANCEL,
        area: AREA_REGISTRY_LIST,
    }
}


export function registryMoveFinish() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_FINISH,
        area: AREA_REGISTRY_LIST,
    }
}





export const AREA_REGISTRY_DETAIL = "registryDetail";

export function registryDetailFetchIfNeeded(id) {
    return (dispatch, getState) => {
        dispatch(DetailActions.fetchIfNeeded(AREA_REGISTRY_DETAIL, id, () => {
            return WebApi.getRegistry(id).catch(() => dispatch(registryDetailClear()));
        }));
    }
}

export function registryDetailInvalidate() {
    return DetailActions.invalidate(AREA_REGISTRY_DETAIL, null)
}

export function registryDetailClear() {
    return registryDetailFetchIfNeeded(null);
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

export function registryUpdate(data, callback = null) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.updateRegistry(data)).then(json => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);

            const list = storeFromArea(store, AREA_REGISTRY_LIST);

            if (detail.id == data.id) {
                dispatch(registryDetailInvalidate());
            }

            if (list.filteredRows && indexById(list.filteredRows, data.id) !== null) {
                dispatch(registryListInvalidate())
            }

            if (callback !== null) {
                dispatch(callback());
            }
        });
    }
}

export function registryDelete(id) {
    return (dispatch, getState) => {
        WebApi.deleteRegistry(id).then(() => {
            const store = getState();
            const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
            const list = storeFromArea(store, AREA_REGISTRY_LIST);
            if (detail.id == id) {
                dispatch(registryDetailClear());
            }

            if (list.filteredRows && indexById(list.filteredRows, id) !== null) {
                dispatch(registryListInvalidate())
            }
        });
    }
}

export function registrySetFolder(recordId) {
    return (dispatch, getState) => {
        return WebApi.getRegistry(recordId).then(item => {
            const store = getState();
            const list = storeFromArea(store, AREA_REGISTRY_LIST);

            dispatch(registryListFilter({
                ...list.filter,
                parents: [
                    {id: item.id, name: item.record},
                    ...item.parents
                ],
                typesToRoot: item.typesToRoot,
                text: null,
                registryParentId: item.id,
            }));
        });
    }
}


/// Variant registry


export function registryVariantUpdate(data) {
    return (dispatch, getState) => {
        const store = getState();
        const detail = storeFromArea(store, AREA_REGISTRY_DETAIL);
        let needFetch = false;
        detail.data.variantRecords.map(variant => {
            if (variant.id == data.id && variant.record !== data.record) {
                needFetch = true;
            }
        });
        if (needFetch === true) {
            return savingApiWrapper(dispatch, WebApi.editRegistryVariant(data)).then(json => {
                dispatch(receiveRegistryVariantRecord(json));
            });
        }
    }
}

export function receiveRegistryVariantRecord(json) {
    return {
        item: json,
        type: types.REGISTRY_VARIANT_RECORD_RECEIVED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryVariantAddRow() {
    return{
        type: types.REGISTRY_VARIANT_RECORD_CREATE,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryVariantCreate(data, variantRecordInternalId) {
    return (dispatch) => {
        savingApiWrapper(dispatch, WebApi.addRegistryVariant(data)).then(json => {
            dispatch(registryVariantCreated(json, variantRecordInternalId));
        });
    }
}

export function registryVariantCreated(json, variantRecordInternalId) {
    return {
        json,
        variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_CREATED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryVariantDelete(variantRecordId){
    return (dispatch) => {
        WebApi.deleteVariantRecord(variantRecordId).then(json => {
            dispatch(registryVariantDeleted(variantRecordId));
        });
    }
}

export function registryVariantDeleted(variantRecordId) {
    return {
        variantRecordId,
        type: types.REGISTRY_VARIANT_RECORD_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryVariantInternalDelete(variantRecordInternalId) {
    return {
        variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}


/// Coordinates
export function registryCoordinatesUpdate(data) {
    return (dispatch, getState) => {
        return savingApiWrapper(dispatch, WebApi.updateRegCoordinates(data)).then(json => {
            dispatch(registryCoordinatesReceive(json));
        });
    }
}

export function registryCoordinatesReceive(json) {
    return {
        item: json,
        type: types.REGISTRY_RECORD_COORDINATES_RECEIVED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesAddRow() {
    return {
        type: types.REGISTRY_RECORD_COORDINATES_CREATE,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesCreate(data, coordinatesId) {
    return (dispatch) => {
        savingApiWrapper(dispatch, WebApi.createRegCoordinates(data)).then(json => {
            dispatch(registryCoordinatesCreated(json, coordinatesId));
        });
    }
}

export function registryCoordinatesCreated(json, coordinatesInternalId) {
    return {
        json,
        coordinatesInternalId,
        type: types.REGISTRY_RECORD_COORDINATES_CREATED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesDelete(coordinatesId) {
    return (dispatch) => {
        WebApi.deleteRegCoordinates(coordinatesId).then(json => {
            dispatch(registryCoordinatesDeleted(coordinatesId));
        });
    }
}

export function registryCoordinatesDeleted(coordinatesId) {
    return {
        coordinatesId,
        type: types.REGISTRY_RECORD_COORDINATES_DELETED,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesInternalDelete(coordinatesInternalId) {
    return {
        coordinatesInternalId,
        type: types.REGISTRY_RECORD_COORDINATES_INTERNAL_DELETE,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesChange(item) {
    return {
        item,
        type: types.REGISTRY_RECORD_COORDINATES_CHANGE,
        area: AREA_REGISTRY_DETAIL
    }
}

export function registryCoordinatesUpload(file, regRecordId) {
    return (dispatch) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('regRecordId', regRecordId);
        savingApiWrapper(dispatch, WebApi.regCoordinatesImport(formData)).then(() => {
            dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successCoordinates')));
        }).catch(() => {
            dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorCoordinates')));
        });
    }
}



// Record types

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

