import {WebApi} from 'actions'
import {modalDialogHide} from 'actions/global/modalDialog'

import * as types from 'actions/constants/ActionTypes';

export function visiblePolicyFetchIfNeeded(nodeId, fundVersionId) {
    return (dispatch, getState) => {
        var state = getState();
        var visiblePolicy = state.arrRegion.visiblePolicy;
        if (visiblePolicy == null || visiblePolicy.nodeId != nodeId || visiblePolicy.fundVersionId != fundVersionId ||
            ((!visiblePolicy.fetched || visiblePolicy.dirty) && !visiblePolicy.isFetching)) {
            return dispatch(visiblePolicyFetch(nodeId, fundVersionId));
        }
    }
}

export function visiblePolicyFetch(nodeId, fundVersionId) {
    return dispatch => {
        dispatch(visiblePolicyRequest(nodeId, fundVersionId))
        return WebApi.getVisiblePolicy(nodeId, fundVersionId)
            .then(data => dispatch(visiblePolicyReceive(nodeId, fundVersionId, data)));
    }
}

export function visiblePolicyReceive(nodeId, fundVersionId, data) {
    return {
        type: types.VISIBLE_POLICY_RECEIVE,
        nodeId,
        fundVersionId,
        policyTypeIds: data.policyTypeIdsMap,
        receivedAt: Date.now()
    }
}

export function visiblePolicyRequest(nodeId, fundVersionId) {
    return {
        type: types.VISIBLE_POLICY_REQUEST,
        nodeId,
        fundVersionId
    }
}

export function setVisiblePolicyRequest(nodeId, fundVersionId, policyTypeIdsMap) {
    return dispatch => {
        dispatch({
            type: types.SET_VISIBLE_POLICY_REQUEST,
            nodeId,
            fundVersionId,
            policyTypeIdsMap
        })
        return WebApi.setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap)
            .then(data => {
                dispatch(modalDialogHide());
                dispatch(setVisiblePolicyReceive(nodeId, fundVersionId));
            });
    }
}

export function setVisiblePolicyReceive(nodeId, fundVersionId) {
    return {
        type: types.SET_VISIBLE_POLICY_RECEIVE,
        nodeId,
        fundVersionId
    }
}