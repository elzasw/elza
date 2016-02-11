import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: {
        count:0,
        recordList: []
    }
}

export default function registryRegionList(state = initialState, action) {
    switch (action.type) {
        case types.REF_REGISTRY_LIST_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.REF_REGISTRY_LIST_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
