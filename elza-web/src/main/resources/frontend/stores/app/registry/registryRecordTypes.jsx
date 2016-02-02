/**
 *
 * Store pro záznam / detailu rejstříku
 *
 **/

import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    partyTypeId: undefined,
    item: null,
    LastUpdated: null
}

export default function registryRecordTypes(state = initialState, action = {}) {

    switch (action.type) {
        case types.REGISTRY_RECIVE_REGISTRY_RECORD_TYPES:
            return Object.assign({}, state, {
                item: action.item,
                partyTypeId: action.partyTypeId,
                isFetching: false,
                fetched: true,
                LastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
