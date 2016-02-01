import {WebApi} from 'actions'
import {scopesDataRecive} from 'actions/global/scopesData'


export function requestScopesIfNeeded(versionId = null){
    return (dispatch, getState) => {
        var state = getState();
        var nacteno = false;
        state.refTables.scopesData.scopes.map(scope => {
            if (scope.versionId === versionId){
                nacteno = true;
            }
        });
        if (!nacteno) {
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
