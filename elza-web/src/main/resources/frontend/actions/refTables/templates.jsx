/**
 * Akce pro číselníky šablon.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function templatesFetchIfNeeded(code = null) {
    return (dispatch, getState) => {
        const {refTables:{templates: {items}}} = getState();
        const templates = items[code];
        if (!templates || ((!templates.fetched || templates.dirty) && !templates.isFetching)) {
            return dispatch(templatesFetch(code));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function templatesFetch(code = null) {
    return dispatch => {
        dispatch(templatesRequest(code))
        return WebApi.getTemplates(code)
            .then(json => dispatch(templatesReceive(json, code)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 * @param code string|null kod ke kterému se vztahují templaty
 */
export function templatesReceive(json, code = null) {
    return {
        type: types.REF_TEMPLATES_RECEIVE,
        items: json,
        receivedAt: Date.now(),
        code
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function templatesRequest(code = null) {
    return {
        type: types.REF_TEMPLATES_REQUEST,
        code
    }
}
