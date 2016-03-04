import * as types from 'actions/constants/ActionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'
import subNodeForm from './subNodeForm'
import subNodeFormCache from './subNodeFormCache'
import subNodeRegister from './subNodeRegister'
import subNodeInfo from './subNodeInfo'
import {consolidateState} from 'components/Utils'

var _nextNodeKey = 1;
var _pageSize = 50;

export function nodeInitState(node, prevNodesNode) {
    var result = {
        ...node,
        dirty: false,
        isFetching: false,
        isNodeInfoFetching: false,
        nodeInfoFetched: false,
        nodeInfoDirty: false,
        allChildNodes: [],
        childNodes: [],
        parentNodes: [],
        viewStartIndex: 0,
        pageSize: _pageSize,
        filterText: '',
        searchedIds: {},
        developerScenarios: {
            isFetching: false,
            isDirty: true,
            data: {
                after: [],
                before: [],
                child: []
            }
        }
    };

    if (prevNodesNode) {
        result.nodeKey = prevNodesNode.nodeKey;
        result.subNodeForm = prevNodesNode.subNodeForm;
        result.subNodeFormCache = prevNodesNode.subNodeFormCache;
        result.subNodeInfo = prevNodesNode.subNodeInfo;
        result.subNodeRegister = prevNodesNode.subNodeRegister;
    } else {
        result.nodeKey = _nextNodeKey++;
        result.subNodeForm = subNodeForm(undefined, {type:''});
        result.subNodeFormCache = subNodeFormCache(undefined, {type:''});
        result.subNodeInfo = subNodeInfo(undefined, {type:''});
        result.subNodeRegister = subNodeRegister(undefined, {type:''});
    }

    if (prevNodesNode && prevNodesNode.id == node.id) {
//        result.nodeInfo = prevNodesNode.nodeInfo;
        result.dirty = prevNodesNode.dirty;
        result.isFetching = prevNodesNode.isFetching;
        result.isNodeInfoFetching = prevNodesNode.isNodeInfoFetching;
        result.nodeInfoFetched = prevNodesNode.nodeInfoFetched;
        result.nodeInfoDirty = prevNodesNode.nodeInfoDirty;
        result.childNodes = prevNodesNode.childNodes;
        result.allChildNodes = prevNodesNode.allChildNodes;
        result.parentNodes = prevNodesNode.parentNodes;
        result.selectedSubNodeId = prevNodesNode.selectedSubNodeId;
        result.filterText = prevNodesNode.filterText;
        result.searchedIds = prevNodesNode.searchedIds;
    } else {
//        result.nodeInfo = nodeInfo(undefined, {type:''});
        result.dirty = false;
        result.isFetching = false;
        result.isNodeInfoFetching = false;
        result.nodeInfoFetched = false;
        result.nodeInfoDirty = false;
        result.childNodes = [];
        result.allChildNodes = [];
        result.parentNodes = [];
        result.selectedSubNodeId = null;
        result.filterText = '';
        result.searchedIds = {};
        result.subNodeFormCache = subNodeFormCache(undefined, {type:''})
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
    subNodeFormCache: subNodeFormCache(undefined, {type:''}),
    subNodeRegister: subNodeRegister(undefined, {type:''}),
    subNodeInfo: subNodeInfo(undefined, {type:''}),
    isNodeInfoFetching: false,
    nodeInfoFetched: false,
    nodeInfoDirty: false,
    childNodes: [],
    allChildNodes: [],
    parentNodes: [],
    developerScenarios: {
        isFetching: false,
        isDirty: true,
        data: {
            after: [],
            before: [],
            child: []
        }
    }
}
    //nodeInfo: nodeInfo(undefined, {type:''}),

export function node(state = nodeInitialState, action) {
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                dirty: true,
                isFetching: false,
                isNodeInfoFetching: false,
                nodeInfoFetched: false,
                nodeInfoDirty: false,
                childNodes: [],
                allChildNodes: [],
                parentNodes: [],
                pageSize: _pageSize,
                subNodeForm: subNodeForm(undefined, {type:''}),
                subNodeFormCache: subNodeFormCache(undefined, {type:''}),
                subNodeRegister: subNodeRegister(undefined, {type:''}),
                subNodeInfo: subNodeInfo(undefined, {type:''}),
                nodeKey: _nextNodeKey++,
                developerScenarios: {
                    isFetching: false,
                    isDirty: true,
                    data: {
                        after: [],
                        before: [],
                        child: []
                    }
                }
            }
        case types.STORE_SAVE:
            const {id, name, selectedSubNodeId, viewStartIndex} = state;
            return {
                id,
                name,
                selectedSubNodeId,
                viewStartIndex,
            }
        case types.FA_NODES_REQUEST:
            if (action.nodeMap[state.id]) {
                return {
                    ...state,
                    isFetching: true,
                }
            } else {
                return state
            }        
        case types.FA_NODES_RECEIVE:
            if (action.nodeMap[state.id]) {
                return {
                    ...state,
                    dirty: false,
                    isFetching: false,
                    ...action.nodeMap[state.id]
                }
            } else {
                return state
            }        
        case types.FA_SUB_NODE_FORM_CACHE_RESPONSE:
        case types.FA_SUB_NODE_FORM_CACHE_REQUEST:
            return {
                ...state, 
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
            }
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_POSITION:
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
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_COPY_FROM_PREV_RESPONSE:
            var result = {
                ...state, 
                subNodeForm: subNodeForm(state.subNodeForm, action),
            };
            return consolidateState(state, result);
        case types.CHANGE_NODES:
        case types.CHANGE_FA_RECORD:
            var result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
                subNodeRegister: subNodeRegister(state.subNodeRegister, action),
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
            }
            return consolidateState(state, result);
        case types.FA_FA_SUBNODES_NEXT:
            if ((state.viewStartIndex + state.pageSize/2) < state.allChildNodes.length) {
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
            if ((state.viewStartIndex + state.pageSize) < state.allChildNodes.length) {
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
        case types.FA_FA_SUBNODES_FULLTEXT_SEARCH:
            return {
                ...state,
                filterText: action.filterText
            }
        case types.FA_FA_SUBNODES_FULLTEXT_RESULT:
            if (state.filterText === '') {
                var result = {
                    ...state,
                    childNodes: [...state.allChildNodes],
                    searchedIds: {},
                    viewStartIndex: 0
                }
                result.viewStartIndex = getViewStartIndex(result, state.selectedSubNodeId);
                return result;
            }

            var searchedIds = {}
            action.nodeIds.forEach(n => {
                searchedIds[n.nodeId] = true;
            })

            var childNodes = [];
            state.allChildNodes.forEach(n => {
                if (searchedIds[n.id]) {
                    childNodes.push(n);
                }
            })

            var result = {
                ...state,
                childNodes: childNodes,
                searchedIds: searchedIds,
                viewStartIndex: 0
            }
            result.viewStartIndex = getViewStartIndex(result, state.selectedSubNodeId);
            return result;
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
            var result = {
                ...state,
                subNodeInfo: subNodeInfo(state.subNodeInfo, action),
            }
            return consolidateState(state, result);
        case types.FA_SUB_NODE_REGISTER_REQUEST:
        case types.FA_SUB_NODE_REGISTER_RECEIVE:
        case types.FA_SUB_NODE_REGISTER_VALUE_RESPONSE:
        case types.FA_SUB_NODE_REGISTER_VALUE_DELETE:
        case types.FA_SUB_NODE_REGISTER_VALUE_ADD:
        case types.FA_SUB_NODE_REGISTER_VALUE_CHANGE:
        case types.FA_SUB_NODE_REGISTER_VALUE_FOCUS:
        case types.FA_SUB_NODE_REGISTER_VALUE_BLUR:
            var result = {
                ...state,
                subNodeRegister: subNodeRegister(state.subNodeRegister, action),
            }
            return consolidateState(state, result);
        case types.FA_NODE_INFO_REQUEST:
            return {
                ...state,
                isNodeInfoFetching: true,
            }
        case types.FA_NODE_INFO_RECEIVE:
            var result = {
                ...state,
                isNodeInfoFetching: false,
                nodeInfoFetched: true,
                nodeInfoDirty: false,
                childNodes: action.childNodes,
                allChildNodes: action.childNodes,
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

            // Pokud daná položka není ve filtrovaných položkách, zrušíme filtr
            if (action.subNodeId !== null && indexById(state.childNodes, action.subNodeId) === null) {
                result.filterText = ''
                result.searchedIds = {}
                result.childNodes = [...state.allChildNodes]
                result.viewStartIndex = 0;
            }

            // Změna view tak, aby byla daná položka vidět
            if (action.subNodeId !== null) {
                result.viewStartIndex = getViewStartIndex(result, action.subNodeId);
            }

            // Data vztahující se k vybranému ID
            if (state.selectedSubNodeId != action.subNodeId) {
                result.subNodeRegister = subNodeRegister(undefined, {type:''});
                result.subNodeForm = subNodeForm(undefined, {type:''});
                result.subNodeInfo = subNodeInfo(undefined, {type:''});
            }
            return result;
        case types.CHANGE_CONFORMITY_INFO:
            return Object.assign({}, state, { nodeInfoDirty: true });

        case types.CHANGE_ADD_LEVEL:
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
                    var nodeIndex = indexById(state.allChildNodes, action.indexNode.id);
                    if (nodeIndex != null) {
                        switch (action.direction) {
                            case "AFTER":
                            case "BEFORE":
                                nodeIndex = action.direction == "BEFORE" ? nodeIndex : nodeIndex + 1;

                                var allChildNodes = [
                                    ...state.allChildNodes.slice(0, nodeIndex),
                                    nodeInitState(action.newNode),
                                    ...state.allChildNodes.slice(nodeIndex)
                                ]

                                return {
                                    ...state,
                                    version: action.parentNode.version,
                                    allChildNodes: allChildNodes,
                                    childNodes: [...allChildNodes],
                                    filterText: '',
                                    searchedIds: {}
                                };
                            case "CHILD":
                                var allChildNodes = [
                                    ...state.allChildNodes,
                                    nodeInitState(action.newNode)
                                ]

                                return {
                                    ...state,
                                    version: action.parentNode.version,
                                    allChildNodes: allChildNodes,
                                    childNodes: [...allChildNodes],
                                    filterText: '',
                                    searchedIds: {}
                                };
                        }
                    }
                    return {
                        ...state,
                        version: action.parentNode.version,
                    };
                case "DELETE":
                    var nodeIndex = indexById(state.childNodes, action.node.id);
                    if (nodeIndex != null) {
                        return {
                            ...state,
                            version: action.parentNode.version,
                            childNodes: [
                                ...state.childNodes.slice(0, nodeIndex),
                                ...state.childNodes.slice(nodeIndex + 1)
                            ]
                        };
                    }
                    return {
                        ...state,
                        version: action.parentNode.version,
                    };
                default:
                    return state;
            }
        case types.DEVELOPER_SCENARIOS_RECEIVED:
            return {
                ...state,
                developerScenarios: {
                    isFetching: false,
                    isDirty: false,
                    data: action.data
                }
            };
        case types.DEVELOPER_SCENARIOS_DIRTY:
            return {
                ...state,
                developerScenarios: {
                    ...state.developerScenarios,
                    isDirty: true
                }
            };
        case types.DEVELOPER_SCENARIOS_FETCHING:
            return {
                ...state,
                developerScenarios: {
                    ...state.developerScenarios,
                    isFetching: true
                }
            };
        default:
            return state;
    }
}
