import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function fetchFaFileTreeIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.faFileTree.fetched && !state.faFileTree.isFetching) {
            return dispatch(fetchFaFileTree());
        }
    }
}

export function fetchFaFileTree() {
    return dispatch => {
        dispatch(requestFaFileTree())
        return WebApi.getFaFileTree()
            .then(json => dispatch(receiveFaFileTree(json)));
    }
}

export function receiveFaFileTree(json) {
    return {
        type: types.FA_RECEIVE_FA_FILE_TREE,
        items: json,
        receivedAt: Date.now()
    }
}

export function requestFaFileTree() {
    return {
        type: types.FA_REQUEST_FA_FILE_TREE
    }
}
