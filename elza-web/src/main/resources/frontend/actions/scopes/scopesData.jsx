import {WebApi} from 'actions'
import {scopesDataRecive} from 'actions/global/scopesData'

export function requestScopesIfNeeded(versionId = null){

    return (dispatch, getState) => {
        var state = getState();
        if (true) {
            return dispatch(requestScopes(versionId));
        }
    }
}




export function requestScopes(versionId = null) {
    return dispatch => {
        return WebApi.getScopes(versionId)
            .then((json) => {
                dispatch(scopesDataRecive(versionId, json));
            });
    }
}