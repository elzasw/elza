import * as types from 'actions/constants/ActionTypes.js';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'
import subNodeForm from './subNodeForm.jsx'
import subNodeFormCache from './subNodeFormCache.jsx'
import subNodeRegister from './subNodeRegister.jsx'
import subNodeDaos from './subNodeDaos.jsx'
import subNodeInfo from './subNodeInfo.jsx'
import {consolidateState} from 'components/Utils.jsx'
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx'
import {isSubNodeRegisterAction} from 'actions/arr/subNodeRegister.jsx'
import {isSubNodeDaosAction} from 'actions/arr/subNodeDaos.jsx'

let _nextRoutingKey = 1;
const _routingKeyAreaPrefix = 'NODE|';
const _pageSize = 50;

export function nodeInitState(node, prevNodesNode) {
    var result = {
        ...node,
        dirty: false,
        isFetching: false,
        isNodeInfoFetching: false,
        nodeInfoFetched: false,
        nodeInfoDirty: false,
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
        result.routingKey = prevNodesNode.routingKey;
        result.subNodeForm = prevNodesNode.subNodeForm;
        result.subNodeFormCache = prevNodesNode.subNodeFormCache;
        result.subNodeInfo = prevNodesNode.subNodeInfo;
        result.subNodeRegister = prevNodesNode.subNodeRegister;
        result.subNodeDaos = prevNodesNode.subNodeDaos;
    } else {
        result.routingKey = _routingKeyAreaPrefix + _nextRoutingKey++;
        result.subNodeForm = subNodeForm(undefined, {type:''});
        result.subNodeFormCache = subNodeFormCache(undefined, {type:''});
        result.subNodeInfo = subNodeInfo(undefined, {type:''});
        result.subNodeRegister = subNodeRegister(undefined, {type:''});
        result.subNodeDaos = subNodeDaos();
    }

    if (prevNodesNode && prevNodesNode.id == node.id) {
//        result.nodeInfo = prevNodesNode.nodeInfo;
        result.dirty = prevNodesNode.dirty;
        result.isFetching = prevNodesNode.isFetching;
        result.isNodeInfoFetching = prevNodesNode.isNodeInfoFetching;
        result.nodeInfoFetched = prevNodesNode.nodeInfoFetched;
        result.nodeInfoDirty = prevNodesNode.nodeInfoDirty;
        result.childNodes = prevNodesNode.childNodes;
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
        result.parentNodes = [];
        result.selectedSubNodeId = null;
        result.filterText = '';
        result.searchedIds = {};
        result.subNodeFormCache = subNodeFormCache(undefined, {type:''})
    }

    return result;
}

function getViewStartIndex(state, selectedId) {
    const {pageSize, viewStartIndex, childNodes} = state;
    const index = indexById(childNodes, selectedId);
    if (index !== null) {   // null může být, pokud nejsou data seznamu položek accordionu (childNodes) ještě načtena
        if (index < viewStartIndex || index >= viewStartIndex + pageSize) {
            let newIndex = index - Math.floor(pageSize / 2);
            let lastPageIndex = childNodes.length - pageSize;
            if(newIndex > lastPageIndex){
                newIndex = lastPageIndex;
            }
            return Math.max(newIndex, 0)
        }
    }
    return state.viewStartIndex;
}

const nodeInitialState = {
    id: null,
    name: null,
    routingKey: _routingKeyAreaPrefix + _nextRoutingKey++,
    selectedSubNodeId: null,
    subNodeForm: subNodeForm(undefined, {type:''}),
    subNodeFormCache: subNodeFormCache(undefined, {type:''}),
    subNodeRegister: subNodeRegister(undefined, {type:''}),
    subNodeDaos: subNodeDaos(),
    subNodeInfo: subNodeInfo(undefined, {type:''}),
    isNodeInfoFetching: false,
    nodeInfoFetched: false,
    nodeInfoDirty: false,
    childNodes: [],
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
    if (nodeFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            subNodeForm: subNodeForm(state.subNodeForm, action),
        };
        return consolidateState(state, result);
    }

    if (nodeFormActions.isSubNodeFormCacheAction(action)) {
        return {
            ...state,
            subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
        }
    }

    if (isSubNodeInfoAction(action)) {
        const result = {
            ...state,
            subNodeInfo: subNodeInfo(state.subNodeInfo, action),
        };
        return consolidateState(state, result);
    }

    if (isSubNodeRegisterAction(action)) {
        const result = {
            ...state,
            subNodeRegister: subNodeRegister(state.subNodeRegister, action),
        };
        return consolidateState(state, result);
    }

    if (isSubNodeDaosAction(action)) {
        const result = {
            ...state,
            subNodeDaos: subNodeDaos(state.subNodeDaos, action),
        };
        return consolidateState(state, result);
    }

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
                parentNodes: [],
                pageSize: _pageSize,
                subNodeForm: subNodeForm(undefined, {type:''}),
                subNodeFormCache: subNodeFormCache(undefined, {type:''}),
                subNodeRegister: subNodeRegister(undefined, {type:''}),
                subNodeDaos: subNodeDaos(),
                subNodeInfo: subNodeInfo(undefined, {type:''}),
                routingKey: _routingKeyAreaPrefix + _nextRoutingKey++,
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
        case types.FUND_NODES_REQUEST:
            if (action.nodeMap[state.id]) {
                return {
                    ...state,
                    isFetching: true,
                }
            } else {
                return state
            }
        case types.FUND_NODES_RECEIVE:
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
        case types.FUND_NODE_INCREASE_VERSION:
            var result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),    // změna pro formulář, pokud je potřeba
            }
            if (result.id === action.nodeId && result.version === action.nodeVersionId) { // změníme aktuální node
                result.version = result.version + 1;
            }
            for (let a=0; a<result.childNodes.length; a++) {
                if (result.childNodes[a].id === action.nodeId && result.childNodes[a].version === action.nodeVersionId) {   // změna tohoto node
                    result.childNodes = [
                        ...result.childNodes.slice(0, a),
                        {
                            ...result.childNodes[a],
                            version: result.childNodes[a].version + 1
                        },
                        ...result.childNodes.slice(a + 1)
                    ]
                    break;
                }
            }
            return consolidateState(state, result);
        case types.CHANGE_NODES:
        case types.CHANGE_FUND_RECORD:
            var result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
                subNodeRegister: subNodeRegister(state.subNodeRegister, action),
                subNodeDaos: subNodeDaos(state.subNodeDaos, action),
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
            }
            return consolidateState(state, result);
        case types.FUND_FUND_CHANGE_READ_MODE:
            var result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
            }
            return consolidateState(state, result);
        case types.FUND_FUND_SUBNODES_NEXT:
            if ((state.viewStartIndex + state.pageSize/2) < state.childNodes.length) {
                return {
                    ...state,
                    viewStartIndex: state.viewStartIndex + state.pageSize/2
                }
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_PREV:
            if (state.viewStartIndex > 0) {
                return {
                    ...state,
                    viewStartIndex: Math.max(state.viewStartIndex - state.pageSize/2, 0)
                }
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_NEXT_PAGE:
            if ((state.viewStartIndex + state.pageSize) < state.childNodes.length) {
                return {
                    ...state,
                    viewStartIndex: state.viewStartIndex + state.pageSize
                }
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_PREV_PAGE:
            if (state.viewStartIndex > 0) {
                return {
                    ...state,
                    viewStartIndex: Math.max(state.viewStartIndex - state.pageSize, 0)
                }
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH:
            return {
                ...state,
                filterText: action.filterText
            }
        case types.FUND_FUND_SUBNODES_FULLTEXT_RESULT:
            if (state.filterText === '') {
                var result = {
                    ...state,
                    nodeInfoDirty: true,
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
            state.childNodes.forEach(n => {
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
        case types.FUND_NODE_INFO_REQUEST:
            return {
                ...state,
                isNodeInfoFetching: true,
            }
        case types.FUND_NODE_INFO_RECEIVE:
            var result = {
                ...state,
                isNodeInfoFetching: false,
                nodeInfoFetched: true,
                nodeInfoDirty: false,
                childNodes: action.childNodes,
                parentNodes: action.parentNodes,
                lastUpdated: action.receivedAt
            }

            // Změna view tak, aby byla daná položka vidět
            if (state.selectedSubNodeId !== null) {
                result.viewStartIndex = getViewStartIndex(result, state.selectedSubNodeId);
            }

            return result;
        case types.FUND_FUND_SELECT_SUBNODE:
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
                result.childNodes = [...state.childNodes]
                result.viewStartIndex = 0;
            }

            // Změna view tak, aby byla daná položka vidět
            if (action.subNodeId !== null) {
                result.viewStartIndex = getViewStartIndex(result, action.subNodeId);
            }

            // Data vztahující se k vybranému ID
            if (state.selectedSubNodeId != action.subNodeId) {
                result.subNodeRegister = subNodeRegister(undefined, {type:''});
                result.subNodeDaos = subNodeDaos();
                result.subNodeForm = subNodeForm(undefined, {type:''});
                result.subNodeInfo = subNodeInfo(undefined, {type:''});
            }
            return result;

        case types.CHANGE_VISIBLE_POLICY:
        case types.CHANGE_CONFORMITY_INFO:
        case types.CHANGE_NODE_REQUESTS:
            return Object.assign({}, state, { nodeInfoDirty: true });

        case types.CHANGE_ADD_LEVEL:
            return Object.assign({}, state, { dirty: true });

        case types.FUND_NODE_CHANGE:
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

                                var childNodes = [
                                    ...state.childNodes.slice(0, nodeIndex),
                                    nodeInitState(action.newNode),
                                    ...state.childNodes.slice(nodeIndex)
                                ]

                                return {
                                    ...state,
                                    version: action.parentNode.version,
                                    childNodes: [...childNodes],
                                    filterText: '',
                                    searchedIds: {}
                                };
                            case "CHILD":
                                var childNodes = [
                                    ...state.childNodes,
                                    nodeInitState(action.newNode)
                                ]

                                return {
                                    ...state,
                                    version: action.parentNode.version,
                                    childNodes: [...childNodes],
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

        case types.FUND_INVALID:
            return consolidateState(state,{
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action)
            });

        case types.NODES_DELETE: {
            let result = {
                ...state
            };

            if (result.selectedSubNodeId != null && action.nodeIds.indexOf(result.selectedSubNodeId) >= 0) {
                result.selectedSubNodeId = null;
            }

            result.subNodeForm = subNodeForm(result.subNodeForm, action);
            result.subNodeFormCache = subNodeFormCache(result.subNodeFormCache, action);

            let childNodes = [];
            result.childNodes.forEach((node) => {
                if (action.nodeIds.indexOf(node.id) < 0) {
                    childNodes.push(node);
                }
            });
            result.childNodes = childNodes;

            return consolidateState(state, result);
        }

        default:
            return state;
    }
}
