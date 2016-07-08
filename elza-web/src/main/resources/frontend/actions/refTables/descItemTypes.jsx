import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function descItemTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.descItemTypes.fetched || state.refTables.descItemTypes.dirty) && !state.refTables.descItemTypes.isFetching) {
            return dispatch(descItemTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function descItemTypesFetch() {
    return dispatch => {
        dispatch(descItemTypesRequest())
        return WebApi.getDescItemTypes()
            .then(json => dispatch(descItemTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function descItemTypesReceive(json) {
    return {
        type: types.REF_DESC_ITEM_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function descItemTypesRequest() {
    return {
        type: types.REF_DESC_ITEM_TYPES_REQUEST
    }
}
