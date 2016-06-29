/**
 * Akce pro číselníky šablon.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function templatesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.templates.fetched || state.refTables.templates.dirty) && !state.refTables.templates.isFetching) {
            return dispatch(templatesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function templatesFetch() {
    return dispatch => {
        dispatch(templatesRequest())
        return WebApi.getTemplates()
            .then(json => dispatch(templatesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function templatesReceive(json) {
    return {
        type: types.REF_TEMPLATES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function templatesRequest() {
    return {
        type: types.REF_TEMPLATES_REQUEST
    }
}
