/**
 * Akce pro obaly.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {fundSubNodeFormValueChange, fundSubNodeFormValueBlur} from 'actions/arr/subNodeForm.jsx'

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {int} fundId id AS
 */
export function packetsFetchIfNeeded(fundId) {
    return (dispatch, getState) => {
        var state = getState();
        var packets = state.arrRegion.packets[fundId];
        if (packets == null || ((!packets.fetched || packets.dirty) && !packets.isFetching)) {
            return dispatch(packetsFetch(fundId));
        }
    }
}

/**
 * Nové načtení dat.
 * @param {int} fundId id AS
 */
export function packetsFetch(fundId) {
    return dispatch => {
        dispatch(packetsRequest(fundId))
        return WebApi.getPackets(fundId)
            .then(items => dispatch(packetsReceive(fundId, items)));
    }
}

/**
 * Nová data byla načtena.
 * @param {int} fundId id AS
 * @param {Object} items načtený seznam obalů
 */
export function packetsReceive(fundId, items) {
    return {
        type: types.PACKETS_RECEIVE,
        fundId: fundId,
        items: items,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {int} fundId id AS
 */
export function packetsRequest(fundId) {
    return {
        type: types.PACKETS_REQUEST,
        fundId: fundId
    }
}

