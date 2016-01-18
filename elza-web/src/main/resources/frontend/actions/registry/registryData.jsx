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
