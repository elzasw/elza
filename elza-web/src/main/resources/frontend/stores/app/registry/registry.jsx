/**
 * 
 * Store pro seznamy rejstříků
 * 
 **/ 
import * as types from 'actions/constants/actionTypes';
import registryData from './registryData';

const initialState = {
    isFetching: false,
    fetched: false,
    selectedId: null,
    isReloadingRegistry: false,
    reloadedRegistry: false,
    search: null,
    registryData: undefined,
    idRegistryParent: null,
    items: [],
    countItems: 0,
}

export default function registry(state = initialState, action) {
    switch (action.type) {
        case types.REGISTRY_SELECT_REGISTRY:
            return Object.assign({}, state, {
                selectedId: action.registry.selectedId,
                reloadedRegistry: false,
                registryData: registryData(state.registryData, action)
            })
        case types.REGISTRY_REQUEST_REGISTRY_LIST:
            return Object.assign({}, state, {
                isFetching: true
            })
        case types.REGISTRY_SEARCH_REGISTRY:
            if (action.registry.search === '')
                action.registry.search = null;
            return Object.assign({}, state, {
                search: action.registry.search,
                fetched: false
            })
        case types.REGISTRY_CHANGED_PARENT_REGISTRY:
            return Object.assign({}, state, {
                idRegistryParent: action.registry.idRegistryParent,
                fetched: false
            })
        case types.REGISTRY_RECEIVE_REGISTRY_LIST:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                items: action.items,
                countItems: action.countItems,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
