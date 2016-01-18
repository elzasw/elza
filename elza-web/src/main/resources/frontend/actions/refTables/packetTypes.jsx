/**
 * Akce pro číselníky typů obalů.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function packetTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.refTables.packetTypes.fetched && !state.refTables.packetTypes.isFetching) {
            return dispatch(packetTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function packetTypesFetch() {
    return dispatch => {
        dispatch(packetTypesRequest())
        return WebApi.getPacketTypes()
            .then(json => dispatch(packetTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function packetTypesReceive(json) {
    return {
        type: types.REF_PACKET_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function packetTypesRequest() {
    return {
        type: types.REF_PACKET_TYPES_REQUEST
    }
}
