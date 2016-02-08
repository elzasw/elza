/**
 * Web api pro komunikaci se serverem.
 */
import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';

export function registryData(registry) {
    return {
        type: types.REGISTRY_SELECT_REGISTRY,
        registry
    }
}

export function registrySearchData(registry) {
    return {
        type: types.REGISTRY_SEARCH_REGISTRY,
        registry
    }
}

export function registryChangeParent(registry) {
    return {
        type: types.REGISTRY_CHANGED_PARENT_REGISTRY,
        registry
    }
}

export function registryChangeDetail(registry) {
    return {
        type: types.REGISTRY_CHANGE_REGISTRY_DETAIL,
        registry
    }
}

export function registryRemoveRegistry(registry) {
    return {
        type: types.REGISTRY_REMOVE_REGISTRY,
        registry
    }
}

export function registryStartMove() {

    return {
        type: types.REGISTRY_MOVE_REGISTRY_START,
    }
}

export function registryStopMove() {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_FINISH
    }
}

export function registryReloadNavigation(){
    return {
        type: types.REGISTRY_RELOAD_NAVIGATION
    }
}

export function registryRecordMove(data){
    return (dispatch) => {
        dispatch(registryRecordUpdate(data, registryStopMove));
    }
}

export function registryRecordUpdate(data, callback = null){
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


export function registryCancelMove(registry) {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_CANCEL,
        registry
    }
}

export function registryUnsetParents() {
    return {
        type: types.REGISTRY_UNSET_PARENT
    }
}

export function registryClearSearch(){
    return {
        type: types.REGISTRY_CLEAR_SEARCH,
    }
}


export function updateRegistryVariantRecord(data){
    return (dispatch, getState) => {
        var state = getState();
        var aktualizovat = false;
        state.registryData.item.variantRecords.map(variant => {
            if (variant.variantRecordId == data.variantRecordId && variant.record !== data.record){
                aktualizovat = true;
            }
        });
        if (aktualizovat === true) {
            return WebApi.editRegistryVariant(data).then(json => {
                dispatch(reciveRegistryVariantRecord(json));
            });
        }
    }
}

export function reciveRegistryVariantRecord(json){
    return {
        item: json,
        type: types.REGISTRY_VARIANT_RECORD_RECIVED
    }
}

export function registryVariantAddRecordRow(){
    return{
        type: types.REGISTRY_VARIANT_RECORD_ADD_NEW_CLEAN
    }
}

export function registryAddVariant(data, variantRecordInternalId){
    return (dispatch) => {
        WebApi.addRegistryVariant(data).then(json => {
            dispatch(registryVariantInserted(json, variantRecordInternalId));
        });
    }
}

export function registryVariantInserted(json, variantRecordInternalId){
    return {
        json: json,
        variantRecordInternalId: variantRecordInternalId,
        type: types.REGISTRY_VARIANT_RECORD_INSERTED
    }
}

export function registryVariantDelete(variantRecordId){
    return (dispatch) => {
        WebApi.deleteVariantRecord(variantRecordId).then(json => {
            dispatch(registryVariantDeleted(variantRecordId));
        });
    }
}

export function registryVariantDeleted(variantRecordId){
    return {
        variantRecordId: variantRecordId,
        type: types.REGISTRY_VARIANT_RECORD_DELETED
    }
}

export function registryVariantInternalDelete(variantRecordInternalId){
    return {
        variantRecordInternalId: variantRecordInternalId,
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
        json: json,
        type: types.REGISTRY_RECORD_NOTE_UPDATED
    };
}