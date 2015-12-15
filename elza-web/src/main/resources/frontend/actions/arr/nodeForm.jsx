import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function nodeFormFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.nodeForm.fetched && !state.nodeForm.isFetching) {
            return dispatch(nodeFormFetch());
        }
    }
}

export function nodeFormFetch() {
    return dispatch => {
        dispatch(nodeFormRequest())
        return WebApi.getnodeForm()
            .then(json => dispatch(nodeFormReceive(json)));
    }
}

export function nodeFormReceive(json) {
    return {
        type: types.FA_NODE_FORM_RECEIVE,
        parentNodes: json.parents,
        childNodes: json.children,
        receivedAt: Date.now()
    }
}

export function nodeFormRequest() {
    return {
        type: types.FA_NODE_FORM_REQUEST
    }
}
