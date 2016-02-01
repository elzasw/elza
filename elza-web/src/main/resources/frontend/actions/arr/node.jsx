import {WebApi} from 'actions';
import * as types from 'actions/constants/actionTypes';
import {faSelectSubNodeInt,faSelectSubNode} from 'actions/arr/nodes';

export function addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes = null, scenarioName = null) {
    return (dispatch) => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version
        };
        indexNode = {
            id: indexNode.id,
            lastUpdate: indexNode.lastUpdate,
            version: indexNode.version
        };
        return WebApi.addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes, scenarioName).then((json) => {
            dispatch(faNodeChange(versionId, {newNode: json, indexNode: indexNode, parentNode: parentNode, direction: direction, action: "ADD"}));
            dispatch(faSelectSubNodeInt(json.id,parentNode));
        });
    }
}

export function faNodeChange(versionId, data) {
    return {
        ...data,
        type: types.FA_NODE_CHANGE,
        versionId
    }
}