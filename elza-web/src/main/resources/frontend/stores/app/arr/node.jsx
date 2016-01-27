import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'
//import nodeInfo from './nodeInfo'
import subNodeForm from './subNodeForm'
import subNodeInfo from './subNodeInfo'

var _nextNodeKey = 1;
var _pageSize = 50;

export function nodeInitState(node, prevNodesNode) {
    var result = {
        id: node.id,
        name: node.name,
        isFetching: false,
        fetched: false,
        dirty: false,
        childNodes: [],
        parentNodes: [],
        viewStartIndex: 0,
        pageSize: _pageSize,
    }

    if (prevNodesNode) {
        result.nodeKey = prevNodesNode.nodeKey;
        result.subNodeForm = prevNodesNode.subNodeForm;
        result.subNodeInfo = prevNodesNode.subNodeInfo;
        result.subNodeInfo = prevNodesNode.subNodeInfo;
    } else {
        result.nodeKey = _nextNodeKey++;
        result.subNodeForm = subNodeForm(undefined, {type:''});
        result.subNodeInfo = subNodeInfo(undefined, {type:''});
    }

    if (prevNodesNode && prevNodesNode.id == node.id) {
//        result.nodeInfo = prevNodesNode.nodeInfo;
        result.isFetching = prevNodesNode.isFetching;
        result.fetched = prevNodesNode.fetched;
        result.dirty = prevNodesNode.dirty;
        result.childNodes = prevNodesNode.childNodes;
        result.parentNodes = prevNodesNode.parentNodes;
        result.selectedSubNodeId = prevNodesNode.selectedSubNodeId;
    } else {
//        result.nodeInfo = nodeInfo(undefined, {type:''});
        result.isFetching = false;
        result.fetched = false;
        result.dirty = false;
        result.childNodes = [];
        result.parentNodes = [];
        result.selectedSubNodeId = null;
    }

    return result;
}

const nodeInitialState = {
    id: null,
    name: null,
    nodeKey: _nextNodeKey++,
    selectedSubNodeId: null,
    subNodeForm: subNodeForm(undefined, {type:''}),
    subNodeInfo: subNodeInfo(undefined, {type:''}),
    isFetching: false,
    fetched: false,
    dirty: false,
    childNodes: [],
    parentNodes: [],
}
    //nodeInfo: nodeInfo(undefined, {type:''}),

export function node(state = nodeInitialState, action) {
    switch (action.type) {
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
        case types.FA_SUB_NODE_FORM_VALUE_BLUR:
        case types.FA_SUB_NODE_FORM_VALUE_FOCUS:
        case types.FA_SUB_NODE_FORM_VALUE_ADD:
        case types.FA_SUB_NODE_FORM_VALUE_DELETE:
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
        case types.FA_SUB_NODE_FORM_VALUE_RESPONSE:
            return Object.assign({}, state, {
                subNodeForm: subNodeForm(state.subNodeForm, action),
            });
        case types.FA_FA_SUBNODES_NEXT:
            if ((state.viewStartIndex + state.pageSize/2) < state.childNodes.length) {
                return {
                    ...state,
                    viewStartIndex: state.viewStartIndex + state.pageSize/2
                }
            } else {
                return state;
            }
        case types.FA_FA_SUBNODES_PREV:
            if (state.viewStartIndex > 0) {
                return {
                    ...state,
                    viewStartIndex: Math.max(state.viewStartIndex - state.pageSize/2, 0)
                }
            } else {
                return state;
            }
        case types.FA_FA_SUBNODES_NEXT_PAGE:
            if ((state.viewStartIndex + state.pageSize) < state.childNodes.length) {
                return {
                    ...state,
                    viewStartIndex: state.viewStartIndex + state.pageSize
                }
            } else {
                return state;
            }
        case types.FA_FA_SUBNODES_PREV_PAGE:
            if (state.viewStartIndex > 0) {
                return {
                    ...state,
                    viewStartIndex: Math.max(state.viewStartIndex - state.pageSize, 0)
                }
            } else {
                return state;
            }
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
            return Object.assign({}, state, {
                subNodeInfo: subNodeInfo(state.subNodeInfo, action),
            });
        case types.FA_NODE_INFO_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_NODE_INFO_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                childNodes: action.childNodes,
                _childNodes: [...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes, ...action.childNodes],
                parentNodes: action.parentNodes,
                lastUpdated: action.receivedAt
            })
        case types.FA_FA_SELECT_SUBNODE:
            var result = Object.assign({}, state, {
                selectedSubNodeId: action.subNodeId
            });
            if (state.selectedSubNodeId != action.subNodeId) {
                result.subNodeForm = subNodeForm(undefined, {type:''});
                result.subNodeInfo = subNodeInfo(undefined, {type:''});
            }
            return result;

        case types.CHANGE_CONFORMITY_INFO:
            return Object.assign({}, state, { dirty: true });

        default:
            return state;
    }
}
