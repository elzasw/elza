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
    filterText: null,
    registryData: undefined,
    registryParentId: null,
    records: [],
    countRecords: 0,
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
            if (action.registry.filterText === '')
                action.registry.filterText = null;
            return Object.assign({}, state, {
                filterText: action.registry.filterText,
                registryParentId: null,
                fetched: false
            })
            // {...state, filterText: action.registry.filterText, registryParentId: null, fetched: false}

        case types.REGISTRY_CHANGED_PARENT_REGISTRY:
            if (action.registry.filterText === undefined)
                action.registry.filterText = null;
            return Object.assign({}, state, {
                registryParentId: action.registry.registryParentId,
                filterText: action.registry.filterText,
                fetched: false
            })
        case types.REGISTRY_RECEIVE_REGISTRY_LIST:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                records: action.records,
                countRecords: action.countRecords,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
