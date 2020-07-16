import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx';
import subNodeForm from './subNodeForm.jsx';
import subNodeFormCache from './subNodeFormCache.jsx';
import subNodeDaos from './subNodeDaos.jsx';
import subNodeInfo from './subNodeInfo.jsx';
import {consolidateState} from 'components/Utils.jsx';
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx';
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx';
import {isSubNodeDaosAction} from 'actions/arr/subNodeDaos.jsx';

let _nextRoutingKey = 1;
const _routingKeyAreaPrefix = 'NODE|';
const _pageSize = 50; // pro spravnou funkcnost musi byt sude cislo

function initNodeChild(node) {
    return {
        accordionLeft: null,
        accordionRight: null,
        digitizationRequests: null,
        issues: [],
        nodeConformity: {},
        referenceMark: [],
        version: 0,
        ...node,
    }
}

export function nodeInitState(node, prevNodesNode) {
    var result = {
        ...node,
        dirty: false,
        isFetching: false,
        isNodeInfoFetching: false,
        nodeInfoFetched: false,
        nodeInfoDirty: false,
        childNodes: [],
        nodeCount: 0,
        nodeIndex: 0,
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
                child: [],
            },
        },
    };

    if (prevNodesNode) {
        result.routingKey = prevNodesNode.routingKey;
        result.subNodeForm = prevNodesNode.subNodeForm;
        result.subNodeFormCache = prevNodesNode.subNodeFormCache;
        result.subNodeInfo = prevNodesNode.subNodeInfo;
        result.subNodeDaos = prevNodesNode.subNodeDaos;
    } else {
        result.routingKey = _routingKeyAreaPrefix + _nextRoutingKey++;
        result.subNodeForm = subNodeForm(undefined, {type: ''});
        result.subNodeFormCache = subNodeFormCache(undefined, {type: ''});
        result.subNodeInfo = subNodeInfo(undefined, {type: ''});
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
        result.nodeCount = prevNodesNode.nodeCount;
        result.nodeIndex = prevNodesNode.nodeIndex;
        result.parentNodes = prevNodesNode.parentNodes;
        result.selectedSubNodeId = prevNodesNode.selectedSubNodeId;
        result.filterText = prevNodesNode.filterText;
        result.searchedIds = prevNodesNode.searchedIds;
        result.changeParent = false;
    } else {
        //        result.nodeInfo = nodeInfo(undefined, {type:''});
        result.dirty = false;
        result.isFetching = false;
        result.isNodeInfoFetching = false;
        result.nodeInfoFetched = false;
        result.nodeInfoDirty = false;
        result.childNodes = [];
        result.nodeCount = 0;
        result.nodeIndex = 0;
        result.parentNodes = [];
        result.selectedSubNodeId = null;
        result.filterText = '';
        result.searchedIds = {};
        result.subNodeFormCache = subNodeFormCache(undefined, {type: ''});
        result.changeParent = true;
    }

    return result;
}

function getViewStartIndex(index, pageSize) {
    // -1 může být, pokud nejsou data seznamu položek accordionu (childNodes) ještě načtena
    if (index === undefined || index < 0) {   
        throw new Error("invalid index provided");
    }
    // Zajisteni ze "okno" je cele cislo
    const view = Math.floor(pageSize);

    // Ziskani v kolikatem "okne" se polozka nachazi.
    const startIndex = Math.floor(index/view)*view; 

    // Posunuti o "okno" zpet pokud se polozka nachazi v jeho prvni polovine
    if(index - startIndex > view/2){
        return Math.max(startIndex,0);
    } else {
        return Math.max(startIndex-view,0);
    }
}

const nodeInitialState = {
    id: null,
    name: null,
    routingKey: _routingKeyAreaPrefix + _nextRoutingKey++,
    selectedSubNodeId: null,
    subNodeForm: subNodeForm(undefined, {type: ''}),
    subNodeFormCache: subNodeFormCache(undefined, {type: ''}),
    subNodeDaos: subNodeDaos(),
    subNodeInfo: subNodeInfo(undefined, {type: ''}),
    isNodeInfoFetching: false,
    nodeInfoFetched: false,
    nodeInfoDirty: false,
    childNodes: [],
    nodeCount: 0,
    nodeIndex: 0,
    parentNodes: [],
    changeParent: true,
    developerScenarios: {
        isFetching: false,
        isDirty: true,
        data: {
            after: [],
            before: [],
            child: [],
        },
    },
};

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
        };
    }

    if (isSubNodeInfoAction(action)) {
        const result = {
            ...state,
            subNodeInfo: subNodeInfo(state.subNodeInfo, action),
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
                nodeCount: 0,
                nodeIndex: 0,
                parentNodes: [],
                pageSize: _pageSize,
                subNodeForm: subNodeForm(undefined, {type: ''}),
                subNodeFormCache: subNodeFormCache(undefined, {type: ''}),
                subNodeDaos: subNodeDaos(),
                subNodeInfo: subNodeInfo(undefined, {type: ''}),
                routingKey: _routingKeyAreaPrefix + _nextRoutingKey++,
                changeParent: true,
                developerScenarios: {
                    isFetching: false,
                    isDirty: true,
                    data: {
                        after: [],
                        before: [],
                        child: [],
                    },
                },
            };
        case types.STORE_SAVE:
            const {id, name, selectedSubNodeId, viewStartIndex} = state;
            return {
                id,
                name,
                selectedSubNodeId,
                viewStartIndex,
            };
        case types.FUND_NODES_REQUEST:
            if (action.nodeMap[state.id]) {
                return {
                    ...state,
                    isFetching: true,
                };
            } else {
                return state;
            }
        case types.FUND_NODES_RECEIVE:
            if (action.nodeMap[state.id]) {
                return {
                    ...state,
                    dirty: false,
                    isFetching: false,
                    ...action.nodeMap[state.id],
                };
            } else {
                return state;
            }
        case types.FUND_NODE_INCREASE_VERSION:
            var result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action), // změna pro formulář, pokud je potřeba
            };
            if (result.id === action.nodeId && result.version === action.nodeVersionId) {
                // změníme aktuální node
                result.version = result.version + 1;
            }
            for (let a = 0; a < result.childNodes.length; a++) {
                if (
                    result.childNodes[a].id === action.nodeId &&
                    result.childNodes[a].version === action.nodeVersionId
                ) {
                    // změna tohoto node
                    result.childNodes = [
                        ...result.childNodes.slice(0, a),
                        {
                            ...result.childNodes[a],
                            version: result.childNodes[a].version + 1,
                        },
                        ...result.childNodes.slice(a + 1),
                    ];
                    break;
                }
            }
            return consolidateState(state, result);
        case types.CHANGE_NODES:
        case types.CHANGE_FUND_RECORD: {
            let result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
                subNodeDaos: subNodeDaos(state.subNodeDaos, action),
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
            };

            if (action.nodeIds) {
                for (let nodeId of action.nodeIds) {
                    if (indexById(result.childNodes, nodeId) !== null) {
                        result.changeParent = true;
                        break;
                    }
                }
            }

            if (action.nodeId) {
                if (indexById(result.childNodes, action.nodeId) !== null) {
                    result.changeParent = true;
                }
            }

            return consolidateState(state, result);
        }
        case types.FUND_SUBNODE_UPDATE: {
            console.log('UPDATE_CHILD', state, action);
            const data = action.data;
            const node = data.node;

            let childNodes = state.childNodes;
            let index = indexById(childNodes, node && node.id);
            console.log('update index', index);
            let updatedNode = childNodes[index];
            // copy same values from source object
            // not needed for WS update -> should be removed
            // (have to be tested first)
            for (let i in updatedNode) {
                if (typeof data[i] !== 'undefined') {
                    updatedNode[i] = data[i];
                }
            }
            if (updatedNode.accordionLeft != data.formTitle.titleLeft)
                updatedNode.accordionLeft = data.formTitle.titleLeft;
            if (updatedNode.accordionRight != data.formTitle.titleRight)
                updatedNode.accordionRight = data.formTitle.titleRight;
            //console.log("update node", updatedNode);

            var result = {
                ...state,

                subNodeForm: subNodeForm(state.subNodeForm, action),
                subNodeDaos: subNodeDaos(state.subNodeDaos, action),
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
            };
            return consolidateState(state, result);
        }
        case types.FUND_FUND_CHANGE_READ_MODE: {
            var result = {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
            };
            return consolidateState(state, result);
        }
        case types.FUND_FUND_SUBNODES_NEXT: {
            if (state.viewStartIndex + state.pageSize / 2 < state.nodeCount) {
                return {
                    ...state,
                    viewStartIndex: state.viewStartIndex + state.pageSize / 2,
                };
            } else {
                return state;
            }
        }
        case types.FUND_FUND_SUBNODES_PREV: {
            if (state.viewStartIndex > 0) {
                return {
                    ...state,
                    viewStartIndex: Math.max(state.viewStartIndex - state.pageSize / 2, 0),
                };
            } else {
                return state;
            }
        }
        case types.FUND_FUND_SUBNODES_NEXT_PAGE:
            if (state.viewStartIndex + state.pageSize < state.nodeCount) {
                return {
                    ...state,
                    viewStartIndex: state.viewStartIndex + state.pageSize,
                };
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_PREV_PAGE:
            if (state.viewStartIndex > 0) {
                return {
                    ...state,
                    viewStartIndex: Math.max(state.viewStartIndex - state.pageSize, 0),
                };
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH:
            return {
                ...state,
                filterText: action.filterText,
            };
        case types.FUND_NODE_INFO_REQUEST:
            return {
                ...state,
                isNodeInfoFetching: true,
            };
        case types.FUND_NODE_INFO_RECEIVE: {
            let result = {
                ...state,
                dirty: false,
                isNodeInfoFetching: false,
                nodeInfoFetched: true,
                nodeInfoDirty: false,
                changeParent: false,
                childNodes: action.childNodes === null ? state.childNodes : action.childNodes,
                nodeCount: action.nodeCount,
                nodeIndex: action.nodeIndex,
                parentNodes: action.parentNodes === null ? state.parentNodes : action.parentNodes,
                lastUpdated: action.receivedAt,
            };

            // Změna view tak, aby byla daná položka vidět
            if (state.selectedSubNodeId !== null && !action.viewStartIndexInvalidate) {
                result.viewStartIndex = getViewStartIndex(action.nodeIndex, state.pageSize/2);

                // zneplatneni state, aby se data nacetla znovu se spravnymi hodnotami
                if(state.viewStartIndex !== result.viewStartIndex || state.nodeIndex !== action.nodeIndex ){
                    result.nodeInfoDirty = true;
                    result.dirty = true;
                }
            }

            return result;
        }
        case types.FUND_NODE_INFO_INVALIDATE: {
            let result = {
                ...state,
                dirty: true,
                changeParent: true,
                nodeInfoDirty: true,
            };
            return result;
        }

        case types.FUND_FUND_SELECT_SUBNODE:
            if (state.selectedSubNodeId === action.subNodeId) {
                return state;
            }

            var result = {
                ...state,
                selectedSubNodeId: action.subNodeId,
                nodeIndex: action.subNodeIndex,
            };

            // Pokud daná položka není ve filtrovaných položkách, zrušíme filtr
            if (action.subNodeId !== null && indexById(state.childNodes, action.subNodeId) === null) {
                result.filterText = '';
                result.searchedIds = {};
                result.childNodes = [...state.childNodes];
                result.viewStartIndex = 0;
            }

            // Změna view tak, aby byla daná položka vidět
            if (action.subNodeId !== null) {
                result.viewStartIndex = getViewStartIndex(result, action.subNodeId);
            }

            // Data vztahující se k vybranému ID
            if (state.selectedSubNodeId != action.subNodeId) {
                result.subNodeDaos = subNodeDaos();
                result.subNodeForm = subNodeForm(undefined, {type: ''});
                result.subNodeInfo = subNodeInfo(undefined, {type: ''});
            }
            return result;

        case types.CHANGE_VISIBLE_POLICY:
        case types.CHANGE_CONFORMITY_INFO:
        case types.CHANGE_NODE_REQUESTS:
            console.log('change node requests', state, action);
            return Object.assign({}, state, {nodeInfoDirty: true});

        case types.CHANGE_ADD_LEVEL:
            return Object.assign({}, state, {dirty: true, changeParent: true});

        case types.FUND_NODE_CHANGE:
            switch (action.action) {
                // Přidání SubNode
                case 'ADD':
                    /**
                     * Předpokládá v akci attrs
                     * indexNode - Node objekt který slouží k nalezení a před/za který umístímě nový node
                     * parentNode - Parent node
                     * direction - BEFORE/AFTER - před/za
                     * newNode - Node objekt
                     */
                    let nodeIndex = indexById(state.childNodes, action.indexNode.id);

                    const existsNode = indexById(state.childNodes, action.newNode.id);
                    if (existsNode != null) {
                        // JS bylo již přidáno
                        return state;
                    }

                    if (nodeIndex != null) {
                        switch (action.direction) {
                            case 'AFTER':
                            case 'BEFORE': {
                                nodeIndex = action.direction == 'BEFORE' ? nodeIndex : nodeIndex + 1;

                                let childNodes = [
                                    ...state.childNodes.slice(0, nodeIndex),
                                    initNodeChild(action.newNode),
                                    ...state.childNodes.slice(nodeIndex),
                                ];

                                return {
                                    ...state,
                                    version: action.parentNode.version,
                                    childNodes: [...childNodes],
                                    filterText: '',
                                    searchedIds: {},
                                };
                            }
                            case 'CHILD': {
                                let childNodes = [...state.childNodes, initNodeChild(action.newNode),
                                ];

                                return {
                                    ...state,
                                    version: action.parentNode.version,
                                    childNodes: [...childNodes],
                                    filterText: '',
                                    searchedIds: {},
                                };
                            }
                            default:
                                break;
                        }
                    }
                    return {
                        ...state,
                        version: action.parentNode.version,
                    };
                case 'DELETE': {
                    let nodeIndex = indexById(state.childNodes, action.node.id);
                    if (nodeIndex != null) {
                        return {
                            ...state,
                            version: action.parentNode.version,
                            childNodes: [
                                ...state.childNodes.slice(0, nodeIndex),
                                ...state.childNodes.slice(nodeIndex + 1),
                            ],
                        };
                    }
                    return {
                        ...state,
                        version: action.parentNode.version,
                    };
                }
                default:
                    return state;
            }
        case types.DEVELOPER_SCENARIOS_RECEIVED:
            return {
                ...state,
                developerScenarios: {
                    isFetching: false,
                    isDirty: false,
                    data: action.data,
                },
            };
        case types.DEVELOPER_SCENARIOS_DIRTY:
            return {
                ...state,
                developerScenarios: {
                    ...state.developerScenarios,
                    isDirty: true,
                },
            };
        case types.DEVELOPER_SCENARIOS_FETCHING:
            return {
                ...state,
                developerScenarios: {
                    ...state.developerScenarios,
                    isFetching: true,
                },
            };

        case types.FUND_INVALID:
            return consolidateState(state, {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
                subNodeFormCache: subNodeFormCache(state.subNodeFormCache, action),
            });

        case types.NODES_DELETE: {
            let result = {
                ...state,
            };

            if (result.selectedSubNodeId != null && action.nodeIds.indexOf(result.selectedSubNodeId) >= 0) {
                result.selectedSubNodeId = null;
            }

            result.subNodeForm = subNodeForm(result.subNodeForm, action);
            result.subNodeFormCache = subNodeFormCache(result.subNodeFormCache, action);

            let childNodes = [];
            result.childNodes.forEach(node => {
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
