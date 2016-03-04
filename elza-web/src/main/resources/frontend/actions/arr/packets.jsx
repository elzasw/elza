/**
 * Akce pro obaly.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {faSubNodeFormValueChange, faSubNodeFormValueBlur} from 'actions/arr/subNodeForm'

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {int} findingAidId id AP
 */
export function packetsFetchIfNeeded(findingAidId) {
    return (dispatch, getState) => {
        var state = getState();
        var packets = state.arrRegion.packets[findingAidId];
        if (packets == null || ((!packets.fetched || packets.dirty) && !packets.isFetching)) {
            return dispatch(packetsFetch(findingAidId));
        }
    }
}

/**
 * Nové načtení dat.
 * @param {int} findingAidId id AP
 */
export function packetsFetch(findingAidId) {
    return dispatch => {
        dispatch(packetsRequest(findingAidId))
        return WebApi.getPackets(findingAidId)
            .then(items => dispatch(packetsReceive(findingAidId, items)));
    }
}

/**
 * Nová data byla načtena.
 * @param {int} findingAidId id AP
 * @param {Object} items načtený seznam obalů
 */
export function packetsReceive(findingAidId, items) {
    return {
        type: types.PACKETS_RECEIVE,
        findingAidId: findingAidId,
        items: items,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {int} findingAidId id AP
 */
export function packetsRequest(findingAidId) {
    return {
        type: types.PACKETS_REQUEST,
        findingAidId: findingAidId
    }
}

/**
 * Byl vytvořen nový obal, akce pro informování o jeho vytvoření.
 * @param {int} findingAidId id AP
 * @param {Object} data nově vytvořeného obalu
 */
export function createPacketReceive(findingAidId, data) {
    return {
        type: types.CREATE_PACKET_RECEIVE,
        data: data,
        findingAidId: findingAidId
    }
}

/**
 * Vytvoření nového obalu.
 * @param {int} findingAidId id AP
 * @param {string} storageNumber ukládací číslo
 * @param {int} packetTypeId id typu obalu
 * @param {bool} invalidPacket je obal nevalidní?
 * @param {Object} valueLocation konkrétní umístění hodnoty ve formuláři
 * @param {int} versionId verze AP
 * @param {int} selectedSubNodeId pod jakým uzlem bylo editováno - hodnota se edituje pod uzlem a z editace je možné založit nový obal
 * @param {int} nodeKey klíč záložky
 */
export function createPacket(findingAidId, storageNumber, packetTypeId, invalidPacket, valueLocation, versionId, selectedSubNodeId, nodeKey) {
    return dispatch => {
        return WebApi.insertPacket(findingAidId, storageNumber, packetTypeId, invalidPacket)
                .then(json => dispatch(createPacketReceive(findingAidId, json)))
                .then(action => {
                    dispatch(faSubNodeFormValueChange(versionId, selectedSubNodeId, nodeKey, valueLocation, action.data.id, true));
                    dispatch(modalDialogHide())
                });
    }
}

