/**
 * Web api pro komunikaci se serverem.
 */
import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

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

export function registryStopMove(registry) {
    return {
        type: types.REGISTRY_MOVE_REGISTRY_FINISH,
        registry
    }
}

export function registryUpdated() {
    return {
        type: types.REGISTRY_UPDATED,

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