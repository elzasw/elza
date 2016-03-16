/**
 * Akce pro obaly.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {fundSubNodeFormValueChange, fundSubNodeFormValueBlur} from 'actions/arr/subNodeForm'

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

/**
 * Byl vytvořen nový obal, akce pro informování o jeho vytvoření.
 * @param {int} fundId id AS
 * @param {Object} data nově vytvořeného obalu
 */
export function createPacketReceive(fundId, data) {
    return {
        type: types.CREATE_PACKET_RECEIVE,
        data: data,
        fundId: fundId
    }
}

/**
 * Vytvoření nového obalu.
 * @param {int} fundId id AS
 * @param {string} storageNumber ukládací číslo
 * @param {int} packetTypeId id typu obalu
 * @param {bool} invalidPacket je obal nevalidní?
 * @param {Object} valueLocation konkrétní umístění hodnoty ve formuláři
 * @param {int} versionId verze AS
 * @param {int} selectedSubNodeId pod jakým uzlem bylo editováno - hodnota se edituje pod uzlem a z editace je možné založit nový obal
 * @param {int} nodeKey klíč záložky
 */
export function createPacket(fundId, storageNumber, packetTypeId, invalidPacket, valueLocation, versionId, selectedSubNodeId, nodeKey) {
    return dispatch => {
        return WebApi.insertPacket(fundId, storageNumber, packetTypeId, invalidPacket)
                .then(json => dispatch(createPacketReceive(fundId, json)))
                .then(action => {
                    dispatch(fundSubNodeFormValueChange(versionId, selectedSubNodeId, nodeKey, valueLocation, action.data.id, true));
                    dispatch(modalDialogHide())
                });
    }
}

