import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'
//import nodeInfo from './nodeInfo'
import subNodeForm from './subNodeForm'
import subNodeRegister from './subNodeRegister'
import subNodeInfo from './subNodeInfo'
import {consolidateState} from 'components/Utils'

var _nextNodeKey = 1;
var _pageSize = 50;

export function nodeInitState(node, prevNodesNode) {
    var result = {
        ...node,
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
        result.subNodeRegister = prevNodesNode.subNodeRegister;
    } else {
        result.nodeKey = _nextNodeKey++;
        result.subNodeForm = subNodeForm(undefined, {type:''});
        result.subNodeInfo = subNodeInfo(undefined, {type:''});
        result.subNodeRegister = subNodeRegister(undefined, {type:''});
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

function getViewStartIndex(state, selectedId) {
    var index = indexById(state.childNodes, selectedId)
    if (index !== null) {   // null může být, pokud nejsou data seznamu položek accordionu (childNodes) ještě načtena
        if (index < state.viewStartIndex || index >= state.viewStartIndex + state.pageSize) {
            return state.pageSize * Math.floor(index / state.pageSize);
        }
    }
    return state.viewStartIndex;
}

const nodeInitialState = {
    id: null,
    name: null,
    nodeKey: _nextNodeKey++,
    selectedSubNodeId: null,
    subNodeForm: subNodeForm(undefined, {type:''}),
    subNodeRegister: subNodeRegister(undefined, {type:''}),
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
        case types.STORE_LOAD:
            return {
                ...state,
                isFetching: false,
                fetched: false,
                dirty: false,
                childNodes: [],
                parentNodes: [],
                pageSize: _pageSize,
                subNodeForm: subNodeForm(undefined, {type:''}),
                subNodeRegister: subNodeRegister(undefined, {type:''}),
                subNodeInfo: subNodeInfo(undefined, {type:''}),
                nodeKey: _nextNodeKey++,
            }
        case types.STORE_SAVE:
            const {id, name, selectedSubNodeId, viewStartIndex} = state;
            return {
                id,
                name,
                selectedSubNodeId,
                viewStartIndex,
            }
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_PARTY:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_RECORD:
        case types.FA_SUB_NODE_FORM_VALUE_VALIDATE_RESULT:
        case types.FA_SUB_NODE_FORM_VALUE_BLUR:
        case types.FA_SUB_NODE_FORM_VALUE_FOCUS:
        case types.FA_SUB_NODE_FORM_VALUE_ADD:
        case types.FA_SUB_NODE_FORM_VALUE_DELETE:
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE:
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_ADD:
        case types.FA_SUB_NODE_FORM_VALUE_RESPONSE:
            var result = {
                ...state, 
                subNodeForm: subNodeForm(state.subNodeForm, action),
            };
            return consolidateState(state, result);
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
            var result = {
                ...state,
                subNodeInfo: subNodeInfo(state.subNodeInfo, action),
            }
            return consolidateState(state, result);
        case types.FA_SUB_NODE_REGISTER_REQUEST:
        case types.FA_SUB_NODE_REGISTER_RECEIVE:
            var result = {
                ...state,
                subNodeRegister: subNodeRegister(state.subNodeRegister, action),
            }
            return consolidateState(state, result);
        case types.FA_NODE_INFO_REQUEST:
            return {
                ...state,
                isFetching: true,
            }
        case types.FA_NODE_INFO_RECEIVE:
            var result = {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                childNodes: action.childNodes,
                parentNodes: action.parentNodes,
                lastUpdated: action.receivedAt
            }

            // Změna view tak, aby byla daná položka vidět
            if (state.selectedSubNodeId !== null) {
                result.viewStartIndex = getViewStartIndex(result, state.selectedSubNodeId);
            }

            return result;
        case types.FA_FA_SELECT_SUBNODE:
            if (state.selectedSubNodeId === action.subNodeId) {
                return state;
            }

            var result = {
                ...state,
                selectedSubNodeId: action.subNodeId
            }

            // Změna view tak, aby byla daná položka vidět
            if (action.subNodeId !== null) {
                result.viewStartIndex = getViewStartIndex(state, action.subNodeId);
            }

            // Data vztahující se k vybranému ID
            if (state.selectedSubNodeId != action.subNodeId) {
                result.subNodeRegister = subNodeRegister(undefined, {type:''});
                result.subNodeForm = subNodeForm(undefined, {type:''});
                result.subNodeInfo = subNodeInfo(undefined, {type:''});
            }
            return result;
        case types.CHANGE_CONFORMITY_INFO:
            return Object.assign({}, state, { dirty: true });
        case types.FA_NODE_CHANGE:
            switch (action.action) {
                // Přidání SubNode
                case "ADD":
                    /**
                     * Předpokládá v akci attrs
                     * indexNode - Node objekt který slouží k nalezení a před/za který umístímě nový node
                     * parentNode - Parent node
                     * direction - BEFORE/AFTER - před/za
                     * newNode - Node objekt
                     */
                    var nodeIndex = indexById(state.childNodes, action.indexNode.id);
                    if (nodeIndex != null) {
                        switch (action.direction) {
                            case "AFTER":
                            case "BEFORE":
                                nodeIndex = action.direction == "BEFORE" ? nodeIndex : nodeIndex + 1;

                                return {
                                    ...state,
                                    childNodes: [
                                        ...state.childNodes.slice(0, nodeIndex),
                                        nodeInitState(action.newNode),
                                        ...state.childNodes.slice(nodeIndex)
                                    ]
                                };
                            case "CHILD":
                                return {
                                    ...state,
                                    childNodes: [
                                        ...state.childNodes,
                                        nodeInitState(action.newNode)
                                    ]
                                };
                        }
                    }
                    return state;
                default:
                    return state;
            }
        default:
            return state;
    }
}
