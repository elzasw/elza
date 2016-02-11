import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils';

export function scopesDataReceive(versionId, data) {
    return {
        type: types.REF_SCOPES_TYPES_RECEIVE,
        versionId: versionId,
        data: data
    };
}

export function scopesFetching(versionId) {
    return {
        type: types.REF_SCOPES_TYPES_FETCHING,
        versionId: versionId
    };
}

export function scopesDirty(versionId) {
    return {
        type: types.REF_SCOPES_TYPES_DIRTY,
        versionId: versionId
    };
}

export function requestScopesIfNeeded(versionId = null) {
    return (dispatch, getState) => {
        var state = getState();

        let index = indexById(state.scopes, versionId, 'versionId');

        if (index != null) {
            if (state.scopes[index].isDirty && !state.scopes[index].isFetching) {
                return dispatch(requestScopes(versionId));
            }
        } else {
            dispatch(scopesFetching(versionId));
            return dispatch(requestScopes(versionId));
        }
    }
}

export function requestScopes(versionId = null) {
    return dispatch => (
        WebApi.getScopes(versionId)
            .then((json) => {
                dispatch(scopesDataReceive(versionId, json));
            })
    )
}