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
    item: null,
    LastUpdated: null
}

export default function recordData(state = initialState, action) {
    switch (action.type) {
        case types.RECORD_SELECT_RECORD:
            if (state.selectedId === action.record.selectedId){
                return state
            }
            else{
                return Object.assign({}, state, {
                    isFetching: false,
                    fetched: false
                })
            }
        case types.RECORD_REQUEST_RECORD_DETAIL:
            return Object.assign({}, state, {
                isFetching: true,
                fetched: false
            })
        case types.RECORD_CHANGE_RECORD_DETAIL:
            return Object.assign({}, state, {
                fetched: false
            })
        case types.RECORD_RECEIVE_RECORD_DETAIL:
            return Object.assign({}, state, {
                selectedId: action.selectedId,
                item: action.item,
                isFetching: false,
                fetched: true,
                LastUpdated: action.receivedAt
            })
        
        default:
            return state
    }
}
