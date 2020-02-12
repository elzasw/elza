/**
 * Akce pro číselníky typů kalendářů.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function calendarTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.calendarTypes.fetched || state.refTables.calendarTypes.dirty) && !state.refTables.calendarTypes.isFetching) {
            return dispatch(calendarTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function calendarTypesFetch() {
    return dispatch => {
        dispatch(calendarTypesRequest())
        return WebApi.getCalendarTypes()
            .then(json => dispatch(calendarTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function calendarTypesReceive(json) {
    return {
        type: types.REF_CALENDAR_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function calendarTypesRequest() {
    return {
        type: types.REF_CALENDAR_TYPES_REQUEST
    }
}
