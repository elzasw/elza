/**
 * 
 * Store pro záznam / detailu rejstříku
 * 
 **/ 

import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    selectedId: null,
    requireReload: false,
    item: null,
    LastUpdated: null
}

export default function registryData(state = initialState, action = {}) {
    switch (action.type) {
        case types.REGISTRY_SELECT_REGISTRY:
            if (state.selectedId === action.registry.selectedId){
                return state
            }
            else{
                return Object.assign({}, state, {
                    isFetching: false,
                    fetched: false,
                    item: action.registry
                })
            }
        case types.REGISTRY_REQUEST_REGISTRY_DETAIL:
            return Object.assign({}, state, {
                isFetching: true,
                fetched: false
            })
        case types.REGISTRY_CHANGE_REGISTRY_DETAIL:
            return Object.assign({}, state, {
                fetched: false
            })
        case types.REGISTRY_RECEIVE_REGISTRY_DETAIL:

            return Object.assign({}, state, {
                selectedId: action.selectedId,
                item: action.item,
                isFetching: false,
                fetched: true,
                requireReload: false,
                LastUpdated: action.receivedAt
            })
        case types.REGISTRY_UPDATED:
            return Object.assign({}, state, {
                requireReload: true,
                fetched: false
            })
        case types.REGISTRY_VARIANT_RECORD_RECIVED:
            console.log("obdrzel jsem data",action);
            console.warn('TODO - provést merge');
            return state
        default:
            return state
    }
}
