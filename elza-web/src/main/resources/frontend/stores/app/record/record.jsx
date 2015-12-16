import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    selectedId: null,
    
    items: []
}

export default function record(state = initialState, action) {
    switch (action.type) {
        case types.RECORD_SELECT_RECORD:
console.log(action);
            return Object.assign({}, state, {
                selectedId: action.record.selectedId,
            })
        case types.RECORD_REQUEST_RECORD_LIST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.RECORD_RECEIVE_RECORD_LIST:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                items: action.items,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
