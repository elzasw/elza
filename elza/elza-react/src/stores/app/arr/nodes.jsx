import * as types from 'actions/constants/ActionTypes';
import {findByRoutingKeyInNodes, indexById, selectedAfterClose} from 'stores/app/utils.jsx';
import {node, nodeInitState} from './node.jsx';
import {consolidateState} from 'components/Utils.jsx';
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx';
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx';
import {isNodeInfoAction} from 'actions/arr/nodeInfo.jsx';
import {isNodeAction} from 'actions/arr/node.jsx';
import {isSubNodeDaosAction} from 'actions/arr/subNodeDaos.jsx';
import {isDeveloperScenariosAction} from 'actions/global/developer.jsx';

const nodesInitialState = {
    activeIndex: null,
    nodes: [],
};

function processNode(state, action, index) {
    if (index != null) {
        var newNode = node(state.nodes[index], action);
        if (newNode !== state.nodes[index]) {
            var result = {
                ...state,
                nodes: [...state.nodes.slice(0, index), newNode, ...state.nodes.slice(index + 1)],
            };
            return consolidateState(state, result);
        } else {
            return state;
        }
    } else {
        return state;
    }
}

export default function nodes(state = nodesInitialState, action) {
    if (
        false ||
        nodeFormActions.isSubNodeFormAction(action) ||
        isSubNodeInfoAction(action) ||
        isNodeInfoAction(action) ||
        isNodeAction(action) ||
        isSubNodeDaosAction(action) ||
        isDeveloperScenariosAction(action) ||
        nodeFormActions.isSubNodeFormCacheAction(action)
    ) {
        if (action.type === types.CHANGE_DAOS) {
            let result = {
                ...state,
                nodes: state.nodes.map(nodeObj => {
                    return node(nodeObj, action);
                }),
            };
            return consolidateState(state, result);
        }

        var r = findByRoutingKeyInNodes(state, action.versionId, action.routingKey);
        if (r) {
            var index = r.nodeIndex;
            return processNode(state, action, index);
        } else {
            return state;
        }
    }

    let nodes;
    let changed;
    switch (action.type) {
        case types.CHANGE_VISIBLE_POLICY:
            changed = false;
            nodes = [...state.nodes];

            for (let i = 0; i < nodes.length; i++) {
                if (action.invalidateNodes === 'ALL' || action.nodeIdsMap[nodes[i].id]) {
                    nodes[i] = node(nodes[i], action);
                    changed = true;
                }
            }
            if (changed) {
                return {...state, nodes};
            }
            return state;
        case types.FUND_NODE_INCREASE_VERSION:
        case types.FUND_FUND_CHANGE_READ_MODE:
            nodes = [...state.nodes];
            for (let i = 0; i < nodes.length; i++) {
                nodes[i] = node(nodes[i], action);
            }
            return {...state, nodes};
        case types.STORE_LOAD:
            return {
                ...state,
                nodes: state.nodes.map(nodeobj => node(nodeobj, action)),
            };
        case types.STORE_SAVE:
            const {activeIndex} = state;
            return {
                activeIndex,
                nodes: state.nodes.map(nodeobj => node(nodeobj, action)),
            };
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST:
        case types.CHANGE_NODES:
            changed = false;
            var newNodes = state.nodes.map(nodeObj => {
                var newNode = node(nodeObj, action);
                if (nodeObj !== newNode) {
                    changed = true;
                }
                return newNode;
            });
            if (changed) {
                return {
                    ...state,
                    nodes: newNodes,
                };
            } else {
                return state;
            }
        case types.FUND_SUBNODE_UPDATE:
            console.log('UPDATE_CHILD - nodes', action);
            let data = action.data;
            nodes = state.nodes;
            var nodesChange = [...nodes];

            changed = false;

            var nodeId;
            if (data.node && data.node.id) {
                nodeId = data.node.id;
                for (var i = 0; i < nodes.length; i++) {
                    console.log(nodes[i].childNodes);
                    var index = indexById(nodes[i].childNodes, nodeId);
                    console.log(index);
                    // změna se ho netýká, vracím původní stav
                    if (index == null) {
                        continue;
                    }

                    var nodeChange = node(nodes[i], action);

                    // nezměnil se stav podřízených, nemusím nic měnit
                    if (nodeChange === nodes[i]) {
                        continue;
                    }

                    changed = true;

                    nodesChange = [...nodesChange.slice(0, i), nodeChange, ...nodesChange.slice(i + 1)];
                }
            }

            if (changed) {
                return {
                    ...state,
                    nodes: nodesChange,
                };
            }

            return state;
        case types.FUND_FUND_SELECT_SUBNODE:
            var newState;

            if (action.subNodeParentNode == null) {
                // jen nulování
                return state;
            }

            // 1. Záložka
            if (action.openNewTab || state.nodes.length == 0) {
                // otevře se vždy nová záložka, pokud není žádná
                // Založíme novou záložku a vybereme ji
                newState = {
                    ...state,
                    nodes: [...state.nodes, nodeInitState(action.subNodeParentNode)],
                    activeIndex: state.nodes.length,
                };
            } else {
                // pokusí se použít aktuální
                var index = state.activeIndex;
                if (state.nodes[index].id !== action.subNodeParentNode.id) {
                    newState = {
                        ...state,
                        nodes: [
                            ...state.nodes.slice(0, index),
                            nodeInitState(action.subNodeParentNode, state.nodes[index]),
                            ...state.nodes.slice(index + 1),
                        ],
                    };
                } else {
                    newState = {...state};
                }
            }

            // 2. Výběr subnode
            var index = newState.activeIndex;
            var newNode = node(newState.nodes[index], action);
            if (newNode !== newState.nodes[index]) {
                return {
                    ...newState,
                    nodes: [...newState.nodes.slice(0, index), newNode, ...newState.nodes.slice(index + 1)],
                };
            } else {
                return consolidateState(state, newState);
            }
        case types.FUND_FUND_CLOSE_NODE_TAB:
            var index = action.index;
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {
                // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.nodes, index);
            } else if (index < state.activeIndex) {
                newActiveIndex--;
            }
            return {
                ...state,
                nodes: [...state.nodes.slice(0, index), ...state.nodes.slice(index + 1)],
                activeIndex: newActiveIndex,
            };
        case types.FUND_FUND_SELECT_NODE_TAB:
            if (state.activeIndex !== action.index) {
                return {
                    ...state,
                    activeIndex: action.index,
                };
            } else {
                return state;
            }
        case types.CHANGE_CONFORMITY_INFO:
        case types.CHANGE_NODE_REQUESTS:
            nodes = state.nodes;
            var nodesChange = [...nodes];

            changed = false;

            var nodeId;
            if (action.nodeIds) {
                for (var j = 0; j < action.nodeIds.length; j++) {
                    nodeId = action.nodeIds[j];
                    for (var i = 0; i < nodes.length; i++) {
                        var index = indexById(nodes[i].childNodes, nodeId);

                        // změna se ho netýká, vracím původní stav
                        if (index == null) {
                            continue;
                        }

                        var nodeChange = node(nodes[i], action);

                        // nezměnil se stav podřízených, nemusím nic měnit
                        if (nodeChange === nodes[i]) {
                            continue;
                        }

                        changed = true;

                        nodesChange = [...nodesChange.slice(0, i), nodeChange, ...nodesChange.slice(i + 1)];
                    }
                }
            }

            if (changed) {
                return {
                    ...state,
                    nodes: nodesChange,
                };
            }

            return state;
        case types.FUND_NODE_CHANGE:
            changed = false;
            nodes = [...state.nodes];

            for (var i = 0; i < nodes.length; i++) {
                if (nodes[i].id == action.parentNode.id) {
                    nodes[i] = node(nodes[i], action);
                    changed = true;
                }
            }
            if (changed) {
                return {...state, nodes};
            }
            return state;
        case types.CHANGE_ADD_LEVEL:
            changed = false;
            nodes = [...state.nodes];

            for (var i = 0; i < nodes.length; i++) {
                if (nodes[i].id == action.parentNodeId) {
                    nodes[i] = node(nodes[i], action);
                    changed = true;
                }
            }
            if (changed) {
                return {...state, nodes};
            }
            return state;
        case types.CHANGE_DELETE_LEVEL:
            changed = false;
            nodes = [...state.nodes];
            for (let i = 0; i < nodes.length; i++) {
                const node = nodes[i];
                if (node.id == action.nodeId || node.id == action.parentNodeId) {
                    node.nodeInfoDirty = true;
                    break;
                }
            }
            return {...state, nodes};
        case types.CHANGE_FUND_RECORD:
            var index = indexById(state.nodes, action.nodeId, 'selectedSubNodeId');
            if (index !== null) {
                nodes = [...state.nodes];
                nodes[index] = node(nodes[index], action);
                return {...state, nodes};
            }
            return state;

        case types.FUND_INVALID:
            let result = {
                ...state,
                nodes: state.nodes.map(nodeObj => {
                    return node(nodeObj, action);
                }),
            };
            return consolidateState(state, result);

        case types.NODES_DELETE: {
            let result = {...state};

            if (state.activeIndex != null) {
                let node = state.nodes[state.activeIndex];
                if (action.nodeIds.indexOf(node.nodeId) >= 0) {
                    result.activeIndex = null;
                }
            }

            nodes = [];
            for (let i = 0; i < result.nodes.length; i++) {
                if (action.nodeIds.indexOf(result.nodes[i].id) < 0) {
                    nodes.push(node(result.nodes[i], action));
                } else {
                    if (result.activeIndex != null) {
                        if (result.activeIndex > i) {
                            result.activeIndex--; // posunutí otevřené záložky
                        } else if (result.activeIndex === 0) {
                            result.activeIndex = null; // zavření jediné záložky
                        }
                    }
                }
            }
            result.nodes = nodes;

            return consolidateState(state, result);
        }

        default:
            return state;
    }
}
