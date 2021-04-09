import {WebApi} from 'actions/index';

import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils';

export function scopesDataReceive(versionId, data) {
    return {
        type: types.REF_SCOPES_TYPES_RECEIVE,
        versionId: versionId,
        data: data,
    };
}

export function scopesFetching(versionId) {
    return {
        type: types.REF_SCOPES_TYPES_FETCHING,
        versionId: versionId,
    };
}

export function scopesDirty(versionId) {
    return {
        type: types.REF_SCOPES_TYPES_DIRTY,
        versionId: versionId,
    };
}

export function requestScopesIfNeeded(versionId = -1) {
    return (dispatch, getState) => {
        var state = getState().refTables.scopesData;

        let index = indexById(state.scopes, versionId, 'versionId');
        if (index !== null) {
            if (state.scopes[index].isDirty && !state.scopes[index].isFetching) {
                return dispatch(requestScopes(versionId));
            }
        } else {
            dispatch(scopesFetching(versionId));
            return dispatch(requestScopes(versionId));
        }
    };
}

export function requestScopes(versionId = -1) {
    return dispatch => {
        const promise = versionId === -1 ? WebApi.getAllScopes() : WebApi.getScopes(versionId);

        promise.then(json => {
            dispatch(scopesDataReceive(versionId, json));
        });
    };
}
