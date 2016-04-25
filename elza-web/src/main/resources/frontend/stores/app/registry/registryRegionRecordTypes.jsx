/**
 *
 * Store pro záznam / detailu rejstříku
 *
 **/

import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    registryTypeId: undefined,
    item: null,
    LastUpdated: null
}

export default function registryRegionRecordTypes(state = initialState, action = {}) {

    switch (action.type) {
        case types.REGISTRY_RECORD_TYPES_RECEIVE:
            return Object.assign({}, state, {
                item: action.item,
                registryTypeId: action.registryTypeId,
                isFetching: false,
                fetched: true,
                LastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
