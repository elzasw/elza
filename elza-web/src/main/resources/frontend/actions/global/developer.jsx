/**
 * Akce pro developer mode.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {barrier} from 'components/Utils.jsx';

export function isDeveloperScenariosAction(action) {
    switch (action.type) {
        case types.DEVELOPER_SCENARIOS_RECEIVED:
        case types.DEVELOPER_SCENARIOS_FETCHING:
        case types.DEVELOPER_SCENARIOS_DIRTY:
            return true
        default:
            return false
    }
}

export function developerSet(enabled) {
    return {
        type: types.DEVELOPER_SET,
        enabled
    }
}

export function developerNodeScenariosRequest(node, versionId) {
    return (dispatch) => {
        dispatch(developerNodeScenariosFetching(node.id, node.key, versionId));
        barrier(
            WebApi.getNodeAddScenarios(node, versionId, "AFTER", true),
            WebApi.getNodeAddScenarios(node, versionId, "BEFORE", true),
            WebApi.getNodeAddScenarios(node, versionId, "CHILD", true)
        )
            .then(data => {
                return {
                    after: data[0].data,
                    before: data[1].data,
                    child: data[2].data
                }
            })
            .then(json => {
                dispatch(developerNodeScenariosReceived(json, node.id, node.key, versionId));
            })
    }
}

export function developerNodeScenariosReceived(data, nodeId, routingKey, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_RECEIVED,
        data,
        nodeId,
        routingKey,
        versionId
    }
}

export function developerNodeScenariosFetching(nodeId, routingKey, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_FETCHING,
        nodeId,
        routingKey,
        versionId
    }
}
export function developerNodeScenariosDirty(nodeId, routingKey, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_DIRTY,
        nodeId,
        routingKey,
        versionId
    }
}