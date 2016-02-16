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
        dispatch(developerNodeScenariosFetching(node.id, versionId));
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
                dispatch(developerNodeScenariosReceived(json, versionId, node.id));
            })
    }
}

export function developerNodeScenariosReceived(data, versionId, nodeId) {
    return {
        type: types.DEVELOPER_SCENARIOS_RECEIVED,
        data,
        nodeId,
        versionId
    }
}

export function developerNodeScenariosFetching(nodeId, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_FETCHING,
        nodeId,
        versionId
    }
}
export function developerNodeScenariosDirty(nodeId, versionId) {
    return {
        type: types.DEVELOPER_SCENARIOS_DIRTY,
        nodeId,
        versionId
    }
}