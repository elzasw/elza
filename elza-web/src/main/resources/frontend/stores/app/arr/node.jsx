import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'
import nodeInfo from './nodeInfo'
import subNodeForm from './subNodeForm'
import subNodeInfo from './subNodeInfo'

var _nextNodeKey = 1;

export function nodeInitState(node, prevNodesNode) {
    var result = {
        id: node.id,
        name: node.name,
    }

    if (prevNodesNode) {
        result.nodeKey = prevNodesNode.nodeKey;
        result.subNodeForm = prevNodesNode.subNodeForm;
        result.subNodeInfo = prevNodesNode.subNodeInfo;
    } else {
        result.nodeKey = _nextNodeKey++;
        result.subNodeForm = subNodeForm(undefined, {type:''});
        result.subNodeInfo = subNodeInfo(undefined, {type:''});
    }

    if (prevNodesNode && prevNodesNode.id == node.id) {
        result.nodeInfo = prevNodesNode.nodeInfo;
        result.selectedSubNodeId = prevNodesNode.selectedSubNodeId;
    } else {
        result.nodeInfo = nodeInfo(undefined, {type:''});
        result.selectedSubNodeId = null;
    }

    return result;
}

const nodeInitialState = {
    id: null,
    name: null,
    nodeKey: _nextNodeKey++,
    selectedSubNodeId: null,
    nodeInfo: nodeInfo(undefined, {type:''}),
    subNodeForm: subNodeForm(undefined, {type:''}),
    subNodeInfo: subNodeInfo(undefined, {type:''}),
}

export function node(state = nodeInitialState, action) {
    switch (action.type) {
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
            return Object.assign({}, state, {
                subNodeForm: subNodeForm(state.subNodeForm, action),
            });
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
            return Object.assign({}, state, {
                subNodeInfo: subNodeInfo(state.subNodeInfo, action),
            });
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
            return Object.assign({}, state, {
                nodeInfo: nodeInfo(state.nodeInfo, action),
            });
        case types.FA_FA_SELECT_SUBNODE:
            var result = Object.assign({}, state, {
                selectedSubNodeId: action.subNodeId
            });
            if (state.selectedSubNodeId !=action.subNodeId) {
                result.subNodeForm = subNodeForm(undefined, {type:''});
                result.subNodeInfo = subNodeInfo(undefined, {type:''});
            }
            return result;
        default:
            return state;
    }
}
