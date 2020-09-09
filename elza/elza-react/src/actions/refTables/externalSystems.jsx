import {WebApi} from 'actions/index.jsx';
import {SimpleListActions} from 'shared/list';
import * as types from 'actions/constants/ActionTypes';

const AREA_AP_EXT_SYSTEM_LIST = 'apExtSystemList';

export function refExternalSystemsFetchIfNeeded() {
    return (dispatch, getState) => {
        const {
            refTables: {externalSystems},
        } = getState();
        if ((!externalSystems.fetched || externalSystems.dirty) && !externalSystems.isFetching) {
            return dispatch(refExternalSystemsFetch());
        }
    };
}

export function refExternalSystemsFetch() {
    return dispatch => {
        dispatch(refExternalSystemsRequest());
        return WebApi.getExternalSystemsSimple().then(json => dispatch(refExternalSystemsReceive(json)));
    };
}

export function refExternalSystemListInvalidate() {
    return SimpleListActions.invalidate(AREA_AP_EXT_SYSTEM_LIST, null);
}

export function refExternalSystemsReceive(json) {
    return {
        type: types.REF_EXTERNAL_SYSTEMS_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

export function refExternalSystemsRequest() {
    return {
        type: types.REF_EXTERNAL_SYSTEMS_REQUEST,
    };
}
