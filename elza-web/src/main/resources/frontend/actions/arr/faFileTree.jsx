import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function faFileTreeFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.faFileTree.fetched && !state.faFileTree.isFetching) {
            return dispatch(faFileTreeFetch());
        }
    }
}

export function faFileTreeFetch() {
    return dispatch => {
        dispatch(faFileTreeRequest())
        return WebApi.getFaFileTree()
            .then(json => dispatch(faFileTreeReceive(json)));
    }
}

export function faFileTreeReceive(json) {
    return {
        type: types.FA_FA_FILE_TREE_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

export function faFileTreeRequest() {
    return {
        type: types.FA_FA_FILE_TREE_REQUEST
    }
}
