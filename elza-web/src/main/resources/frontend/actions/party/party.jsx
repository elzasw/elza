/**
 * Web api pro komunikaci se serverem.
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes'
import {modalDialogHide} from 'actions/global/modalDialog'

export function insertParty(partyType, partyTypeId, nameFormTypeId, nameMain, nameOther, validRange, degreeBefore, degreeAfter) {
    return dispatch => {
        return WebApi.insertParty(partyType, partyTypeId, nameFormTypeId, nameMain, nameOther, degreeBefore, degreeAfter, validRange)
            .then((json) => {
                console.log(json); 
                dispatch(modalDialogHide());
                dispatch(findPartyFetch(''));
                dispatch(partyDetailFetchIfNeeded(json.partyId));
            });
    }
}

export function findPartyFetchIfNeeded(filterText) {
    return (dispatch, getState) => {
        var state = getState();
        console.log(state.partyRegion);
        if (state.partyRegion.filterText !== filterText) {
            return dispatch(findPartyFetch(filterText));
        } else if (!state.partyRegion.fetchedSearch && !state.partyRegion.isFetchingSearh) {
            return dispatch(findPartyFetch(filterText));
        }
    }
}

export function findPartyFetch(filterText) {
    return dispatch => {
        dispatch(findPartyRequest(filterText))
        return WebApi.findParty(filterText)
            .then(json => dispatch(findPartyReceive(filterText, json)));
    }
}

export function findPartyReceive(filterText, json) {
    return {
        type: types.PARTY_FIND_PARTY_RECEIVE,
        items:json,
        filterText: filterText
    }
}

export function findPartyRequest(filterText) {
    return {
        type: types.PARTY_FIND_PARTY_REQUEST,
        filterText: filterText
    }
}

export function partyDetailFetchIfNeeded(selectedPartyID) {
    return (dispatch, getState) => {
        var state = getState();
        if (state.partyRegion.selectedPartyID !== selectedPartyID) {
            return dispatch(partyDetailFetch(selectedPartyID));
        } else if (!state.partyRegion.fetchedDetail && !state.partyRegion.isFetchingDetail) {
            return dispatch(partyDetailFetch(selectedPartyID));
        }
    }
}

export function partyDetailFetch(selectedPartyID) {
    return dispatch => {
        dispatch(partyDetailRequest(selectedPartyID))
        return WebApi.getParty(selectedPartyID)
            .then(json => dispatch(partyDetailReceive(selectedPartyID, json)));
    }
}

export function partyDetailReceive(selectedPartyID, selectedPartyData) {
    return {
        type: types.PARTY_DETAIL_RECEIVE,
        selectedPartyData: selectedPartyData,
        selectedPartyID: selectedPartyID
    }
}

export function partyDetailRequest(selectedPartyID) {
    return {
        type: types.PARTY_DETAIL_REQUEST,
        selectedPartyID: selectedPartyID
    }
}

