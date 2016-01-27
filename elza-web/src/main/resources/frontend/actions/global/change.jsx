import * as types from 'actions/constants/actionTypes';

export function changeConformityInfo(findingAidVersionId, nodeId) {
    return {
        type: types.CHANGE_CONFORMITY_INFO,
        findingAidVersionId: findingAidVersionId,
        nodeId: nodeId
    }
}