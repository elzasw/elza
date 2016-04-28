/**
 * Akce pro registry region data / recordy
 */
import {WebApi} from 'actions/index.jsx';
import {i18n} from 'components/index.jsx';
import {addToastrSuccess,addToastrDanger} from 'components/shared/toastr/ToastrActions.jsx'
import * as types from 'actions/constants/ActionTypes.js';

export function registryRegionDataSelectRecord(registry) {
    return {
        type: types.REGISTRY_RECORD_SELECT,
        registry
    }
}

export function registrySearchData(registry) {
    return {
        type: types.REGISTRY_RECORD_SEARCH,
        registry
    }
}

export function registryChangeParent(registry) {
    return {
        type: types.REGISTRY_PARENT_RECORD_CHANGED,
        registry
    }
}

export function registryChangeDetail(registry) {
    return {
        type: types.REGISTRY_RECORD_DETAIL_CHANGE,
        registry
    }
}

export function registryDeleteRegistry(registry) {
    return {
        type: types.REGISTRY_RECORD_REMOVE,
        registry
    }
}

export function registryStartMove() {

    return {
        type: types.REGISTRY_MOVE_REGISTRY_START
    }
}

export function registryStopMove() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_FINISH
    }
}

export function registryReloadNavigation() {
    return {
        type: types.REGISTRY_RELOAD_NAVIGATION
    }
}

export function registryRecordMove(data) {
    return (dispatch) => {
        dispatch(registryRecordUpdate(data, registryStopMove));
    }
}

export function registryRecordUpdate(data, callback = null) {
    return (dispatch) => {
        WebApi.updateRegistry(data).then(json => {
            dispatch(registryUpdated(json));
            if (callback !== null) {
                dispatch(callback());
            }
        });
    }
}

export function registryUpdated(json) {
    return {
        type: types.REGISTRY_RECORD_UPDATED,
        json: json

    }
}


export function registryCancelMove() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_CANCEL
    }
}

export function registryUnsetParents() {
    return {
        type: types.REGISTRY_PARENT_RECORD_UNSET
    }
}

export function registryClearSearch() {
    return {
        type: types.REGISTRY_CLEAR_SEARCH
    }
}


export function registryVariantUpdate(data) {
    return (dispatch, getState) => {
        const {registryRegion} = getState();
        var needFetch = false;
        registryRegion.registryRegionData.item.variantRecords.map(variant => {
            if (variant.variantRecordId == data.variantRecordId && variant.record !== data.record) {
                needFetch = true;
            }
        });
        if (needFetch === true) {
            return WebApi.editRegistryVariant(data).then(json => {
                dispatch(receiveRegistryVariantRecord(json));
            });
        }
    }
}

export function receiveRegistryVariantRecord(json) {
    return {
        item: json,
        type: types.REGISTRY_VARIANT_RECORD_RECEIVED
    }
}

export function registryVariantAddRow() {
    return{
        type: types.REGISTRY_VARIANT_RECORD_CREATE
    }
}

export function registryVariantCreate(data, variantRecordInternalId) {
    return (dispatch) => {
        WebApi.addRegistryVariant(data).then(json => {
            dispatch(registryVariantCreated(json, variantRecordInternalId));
        });
    }
}

export function registryVariantCreated(json, variantRecordInternalId) {
    return {
        json,
        variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_CREATED
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
        type: types.REGISTRY_VARIANT_RECORD_DELETED
    }
}

export function registryVariantInternalDelete(variantRecordInternalId) {
    return {
        variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_INTERNAL_DELETED
    }
}

export function registryRecordNoteUpdate(data){
    return (dispatch) => {
        WebApi.updateRegistry(data).then(json => {
            dispatch(registryRecordNoteUpdated(json));
        });
    };
}

export function registryRecordNoteUpdated(json){
    return {
        json,
        type: types.REGISTRY_RECORD_NOTE_UPDATED
    };
}

/// Coordinates
export function registryRecordCoordinatesUpdate(data) {
    return (dispatch, getState) => {
        return WebApi.updateRegCoordinates(data).then(json => {
            dispatch(registryRecordCoordinatesReceive(json));
        });
    }
}

export function registryRecordCoordinatesReceive(json) {
    return {
        item: json,
        type: types.REGISTRY_RECORD_COORDINATES_RECEIVED
    }
}

export function registryRecordCoordinatesAddRow() {
    return {
        type: types.REGISTRY_RECORD_COORDINATES_CREATE
    }
}

export function registryRecordCoordinatesCreate(data, coordinatesId) {
    return (dispatch) => {
        WebApi.createRegCoordinates(data).then(json => {
            dispatch(registryRecordCoordinatesCreated(json, coordinatesId));
        });
    }
}

export function registryRecordCoordinatesCreated(json, coordinatesInternalId) {
    return {
        json,
        coordinatesInternalId,
        type: types.REGISTRY_RECORD_COORDINATES_CREATED
    }
}

export function registryRecordCoordinatesDelete(coordinatesId) {
    return (dispatch) => {
        WebApi.deleteRegCoordinates(coordinatesId).then(json => {
            dispatch(registryRecordCoordinatesDeleted(coordinatesId));
        });
    }
}

export function registryRecordCoordinatesDeleted(coordinatesId) {
    return {
        coordinatesId,
        type: types.REGISTRY_RECORD_COORDINATES_DELETED
    }
}

export function registryRecordCoordinatesInternalDelete(coordinatesInternalId) {
    return {
        coordinatesInternalId,
        type: types.REGISTRY_RECORD_COORDINATES_INTERNAL_DELETE
    }
}

export function registryRecordCoordinatesChange(item) {
    return {
        item,
        type: types.REGISTRY_RECORD_COORDINATES_CHANGE
    }
}

export function registryRecordCoordinatesUpload(file, regRecordId) {
    return (dispatch) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('regRecordId', regRecordId);
        WebApi.regCoordinatesImport(formData).then(() => {
            dispatch(addToastrSuccess(i18n('import.toast.success'), i18n('import.toast.successCoordinates')));
        }).catch(() => {
            dispatch(addToastrDanger(i18n('import.toast.error'), i18n('import.toast.errorCoordinates')));
        });
    }
}