import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function packetsFetchIfNeeded(findingAidId) {
    return (dispatch, getState) => {
        var state = getState();
        var packets = state.arrRegion.packets[findingAidId];
        if (packets == null || (!packets.fetched && !packets.isFetching)) {
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
