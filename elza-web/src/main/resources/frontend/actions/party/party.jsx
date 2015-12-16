/**
 * Web api pro komunikaci se serverem.
 */

import * as types from 'actions/constants/actionTypes';

export function findPartyFetchIfNeeded(filterText) {
    return (dispatch, getState) => {
        var state = getState();
        var findParty = state.partyRegion.findParty;

        if (findParty.filterText !== filterText) {
            return dispatch(findPartyFetch(filterText));
        } else if (!findParty.fetched && !findParty.isFetching) {
            return dispatch(findPartyFetch(filterText));
        }
    }
}

export function findPartyFetch(filterText) {
    return dispatch => {
        dispatch(findPartyRequest(filterText))
        return WebApi.getFindParty(filterText)
            .then(json => dispatch(findPartyReceive(filterText, json)));
    }
}

export function findPartyReceive(filterText, json) {
    return {
        party:json
    }
}

export function findPartyRequest(filterText) {
    return {
        filterText: filterText
    }
}