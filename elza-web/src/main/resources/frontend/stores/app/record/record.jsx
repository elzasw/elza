/**
 * 
 * Store pro seznamy rejstříků
 * 
 **/ 
import * as types from 'actions/constants/actionTypes';
import recordData from './recordData';

const initialState = {
    isFetching: false,
    fetched: false,
    selectedId: null,
    isReloadingRecord: false,
    reloadedRecord: false,
    search: null,
    recordData: undefined,
    items: [],
    countItems: 0,
}

export default function record(state = initialState, action) {
    switch (action.type) {
        case types.RECORD_SELECT_RECORD:
            return Object.assign({}, state, {
                selectedId: action.record.selectedId,
                reloadedRecord: false,
                recordData: recordData(state.recordData, action)
            })
        case types.RECORD_REQUEST_RECORD_LIST:
            return Object.assign({}, state, {
                isFetching: true
            })
        case types.RECORD_SEARCH_RECORD:
            return Object.assign({}, state, {
                search: action.record.search,
                fetched: false
            })
        case types.RECORD_RECEIVE_RECORD_LIST:
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
