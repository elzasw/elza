/**
 * Akce pro developer mode.
 */

import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {barrier} from 'components/Utils';

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

export function developerNodeScenariosReceived(data, nodeId, nodeKey, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_RECEIVED,
        data,
        nodeId,
        nodeKey,
        versionId
    }
}

export function developerNodeScenariosFetching(nodeId, nodeKey, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_FETCHING,
        nodeId,
        nodeKey,
        versionId
    }
}
export function developerNodeScenariosDirty(nodeId, nodeKey, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_DIRTY,
        nodeId,
        nodeKey,
        versionId
    }
}