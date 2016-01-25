/**
 * Store pro správu stavu indexování.
 *
 * @author Jiří Vaněk
 * @since 22.1.2016
 */
import * as types from 'actions/constants/actionTypes';

/**
 * Výchozí stav store
 */
const initialState = {
    isFetching: false,
    fetched: false,
    indexing: false
}

export default function fulltext(state = initialState, action = {}) {
    switch (action.type) {
        case types.ADMIN_FULLTEXT_REINDEXING_REQUEST: 
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,            
                indexing: true
            })
            
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_REQUEST: 
            return Object.assign({}, state, {
                isFetching: true,
                fetched: false,            
                indexing: false
            })
            
        case types.ADMIN_FULLTEXT_REINDEXING_STATE_RECIEVE: 
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,            
                indexing: action.indexingState
            })
            
        default: 
            return state;
    }
}