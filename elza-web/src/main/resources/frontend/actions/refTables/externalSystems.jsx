import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

export function refExternalSystemsFetchIfNeeded() {
    return (dispatch, getState) => {
        const {refTables:{externalSystems}} = getState();
        if ((!externalSystems.fetched || externalSystems.dirty) && !externalSystems.isFetching) {
            return dispatch(refExternalSystemsFetch());
        }
    }
}

export function refExternalSystemsFetch() {
    return dispatch => {
        dispatch(refExternalSystemsRequest())
        return WebApi.getExternalSystemsSimple()
            .then(json => dispatch(refExternalSystemsReceive(json)));
    }
}

export function refExternalSystemsReceive(json) {
    return {
        type: types.REF_EXTERNAL_SYSTEMS_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

export function refExternalSystemsRequest() {
    return {
        type: types.REF_EXTERNAL_SYSTEMS_REQUEST
    }
}

export function refExternalSystemsInvalid() {
    return {
        type: types.REF_EXTERNAL_SYSTEMS_INVALID
    }
}
