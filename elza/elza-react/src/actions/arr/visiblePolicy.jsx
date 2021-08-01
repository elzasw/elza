import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes';

export function visiblePolicyFetchIfNeeded(nodeId, fundVersionId) {
    return (dispatch, getState) => {
        const {
            arrRegion: {visiblePolicy},
        } = getState();
        if (
            visiblePolicy == null ||
            visiblePolicy.nodeId !== nodeId ||
            visiblePolicy.fundVersionId !== fundVersionId ||
            ((!visiblePolicy.fetched || visiblePolicy.dirty) && !visiblePolicy.isFetching)
        ) {
            return dispatch(visiblePolicyFetch(nodeId, fundVersionId));
        } else {
            // return current data
            return Promise.resolve({ ...visiblePolicy.otherData });
        }
    };
}

export function visiblePolicyFetch(nodeId, fundVersionId) {
    return dispatch => {
        dispatch(visiblePolicyRequest(nodeId, fundVersionId));
        return WebApi.getVisiblePolicy(nodeId, fundVersionId).then(data =>
            { 
                dispatch(visiblePolicyReceive(nodeId, fundVersionId, data));
                return data;
            }
        );
    };
}

export function visiblePolicyReceive(nodeId, fundVersionId, data) {
    const {policyTypeIdsMap} = data;
    return {
        type: types.VISIBLE_POLICY_RECEIVE,
        nodeId,
        fundVersionId,
        policyTypeIds: policyTypeIdsMap,
        otherData: { ...data },
        receivedAt: Date.now(),
    };
}

export function visiblePolicyRequest(nodeId, fundVersionId) {
    return {
        type: types.VISIBLE_POLICY_REQUEST,
        nodeId,
        fundVersionId,
    };
}

export function setVisiblePolicyRequest(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree, nodeExtensions) {
    return dispatch => {
        dispatch({
            type: types.SET_VISIBLE_POLICY_REQUEST,
            nodeId,
            fundVersionId,
            policyTypeIdsMap,
        });
        return WebApi.setVisiblePolicy(nodeId, fundVersionId, policyTypeIdsMap, includeSubtree, nodeExtensions).then(data => {
            dispatch(setVisiblePolicyReceive(nodeId, fundVersionId));
            return data;
        });
    };
}

export function setVisiblePolicyReceive(nodeId, fundVersionId) {
    return {
        type: types.SET_VISIBLE_POLICY_RECEIVE,
        nodeId,
        fundVersionId,
    };
}
