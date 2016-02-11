import {WebApi} from 'actions'

import * as types from 'actions/constants/ActionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {faSubNodeFormValueChange, faSubNodeFormValueBlur} from 'actions/arr/subNodeForm'

export function packetsFetchIfNeeded(findingAidId) {
    return (dispatch, getState) => {
        var state = getState();
        var packets = state.arrRegion.packets[findingAidId];
        if (packets == null || ((!packets.fetched || packets.dirty) && !packets.isFetching)) {
            return dispatch(packetsFetch(findingAidId));
        }
    }
}

export function packetsFetch(findingAidId) {
    return dispatch => {
        dispatch(packetsRequest(findingAidId))
        return WebApi.getPackets(findingAidId)
            .then(items => dispatch(packetsReceive(findingAidId, items)));
    }
}

export function packetsReceive(findingAidId, items) {
    return {
        type: types.PACKETS_RECEIVE,
        findingAidId: findingAidId,
        items: items,
        receivedAt: Date.now()
    }
}

export function packetsRequest(findingAidId) {
    return {
        type: types.PACKETS_REQUEST,
        findingAidId: findingAidId
    }
}

export function createPacketRequest() {
    return {
        type: types.CREATE_PACKET_REQUEST
    }
}

export function createPacketReceive(findingAidId, data) {
    return {
        type: types.CREATE_PACKET_RECEIVE,
        data: data,
        findingAidId: findingAidId
    }
}

export function createPacket(findingAidId, storageNumber, packetTypeId, invalidPacket, valueLocation, versionId, selectedSubNodeId, nodeKey) {
    return dispatch => {
        dispatch(createPacketRequest())
        return WebApi.insertPacket(findingAidId, storageNumber, packetTypeId, invalidPacket)
                .then(json => dispatch(createPacketReceive(findingAidId, json)))
                .then(action => {
                    dispatch(faSubNodeFormValueChange(versionId, selectedSubNodeId, nodeKey, valueLocation, action.data.id, true));
                    dispatch(modalDialogHide())
                });
    }
}

