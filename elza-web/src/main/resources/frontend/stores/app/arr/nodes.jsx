import * as types from 'actions/constants/ActionTypes';
import {indexById, findByNodeKeyInNodes, selectedAfterClose} from 'stores/app/utils.jsx'
import {node, nodeInitState} from './node.jsx'
import {consolidateState} from 'components/Utils'
import {isSubNodeFormAction} from 'actions/arr/subNodeForm'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo'
import {isNodeInfoAction} from 'actions/arr/nodeInfo'

const nodesInitialState = {
    activeIndex: null,
    nodes: []
}

function processNode(state, action, index) {
    if (index != null) {
        var newNode = node(state.nodes[index], action);
        if (newNode !== state.nodes[index]) {
            var result = {
                ...state,
                nodes: [
                    ...state.nodes.slice(0, index),
                    newNode,
                    ...state.nodes.slice(index + 1)
                ]
            }
            return consolidateState(state, result);
        } else {
            return state;
        }
    } else {
        return state;
    }
}

export default function nodes(state = nodesInitialState, action) {
    if (false
        || isSubNodeFormAction(action)
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
    ) {
        var r = findByNodeKeyInNodes(state, action.versionId, action.nodeKey);
        if (r) {
            var index = r.nodeIndex;
            return processNode(state, action, index);
        } else {
            return state;
        }
    }

    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                nodes: state.nodes.map(nodeobj => node(nodeobj, action))
            }
        case types.STORE_SAVE:
            const {activeIndex} = state;
            return {
                activeIndex,
                nodes: state.nodes.map(nodeobj => node(nodeobj, action))
            }
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST:
        case types.CHANGE_NODES:
            var changed = false;
            var newNodes = state.nodes.map(nodeObj => {
                var newNode = node(nodeObj, action);
                if (nodeObj !== newNode) {
                    changed = true;
                }
                return newNode;
            })
            if (changed) {
                return {
                    ...state,
                    nodes: newNodes
                }
            } else {
                return state
            }
        case types.FUND_SUB_NODE_REGISTER_REQUEST:
        case types.FUND_SUB_NODE_REGISTER_RECEIVE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_DELETE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_ADD:
        case types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_FOCUS:
        case types.FUND_SUB_NODE_REGISTER_VALUE_BLUR:
        case types.FUND_FUND_SUBNODES_FULLTEXT_RESULT:
        case types.DEVELOPER_SCENARIOS_RECEIVED:
        case types.DEVELOPER_SCENARIOS_FETCHING:
        case types.DEVELOPER_SCENARIOS_DIRTY:
            var r = findByNodeKeyInNodes(state, action.versionId, action.nodeKey);
            if (r) {
                var index = r.nodeIndex;
                return processNode(state, action, index);
            } else {
                return state;
            }
        case types.FUND_FUND_SUBNODES_NEXT:
        case types.FUND_FUND_SUBNODES_PREV:
        case types.FUND_FUND_SUBNODES_NEXT_PAGE:
        case types.FUND_FUND_SUBNODES_PREV_PAGE:
        case types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH:
            var index = state.activeIndex;
            return processNode(state, action, index);
        case types.FUND_FUND_SELECT_SUBNODE:
            var newState;

            if (action.subNodeParentNode == null) { // jen nulování
                return state;
            }

            // 1. Záložka
            if (action.openNewTab || state.nodes.length == 0) {   // otevře se vždy nová záložka, pokud není žádná
                // Založíme novou záložku a vybereme ji
                newState = {
                    ...state,
                    nodes: [
                        ...state.nodes,
                        nodeInitState(action.subNodeParentNode)
                    ],
                    activeIndex: state.nodes.length
                }
            } else {    // pokusí se použít aktuální
                var index = state.activeIndex;
                if (state.nodes[index].id !== action.subNodeParentNode.id) {
                    newState = {
                        ...state,
                        nodes: [
                            ...state.nodes.slice(0, index),
                            nodeInitState(action.subNodeParentNode, state.nodes[index]),
                            ...state.nodes.slice(index + 1)
                        ],
                    }
                } else {
                    newState = {...state}
                }
            }

            // 2. Výběr subnode
            var index = newState.activeIndex;
            var newNode = node(newState.nodes[index], action);
            if (newNode !== newState.nodes[index]) {
                return {
                    ...newState,
                    nodes: [
                        ...newState.nodes.slice(0, index),
                        newNode,
                        ...newState.nodes.slice(index + 1)
                    ],
                }
            } else {
                return consolidateState(state, newState);
            }
        case types.FUND_FUND_CLOSE_NODE_TAB:
            var index = action.index;
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.nodes, index);
            } else if (index < state.activeIndex) {
                newActiveIndex--;
            }
            return {
                ...state,
                nodes: [
                    ...state.nodes.slice(0, index),
                    ...state.nodes.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case types.FUND_FUND_SELECT_NODE_TAB:
            if (state.activeIndex !== action.index) {
                return {
                    ...state,
                    activeIndex: action.index
                }
            } else {
                return state;
            }
        case types.CHANGE_CONFORMITY_INFO:
            var nodes = state.nodes;
            var nodesChange = [
                ...nodes
            ];

            var changed = false;

            var nodeId;
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

                    nodesChange = [
                        ...nodesChange.slice(0, i),
                        nodeChange,
                        ...nodesChange.slice(i + 1)
                    ];
                }
            }

            if (changed) {
                return {
                    ...state,
                    nodes: nodesChange,
                }
            }

            return state;
        case types.FUND_NODE_CHANGE:

            var changed = false;
            var nodes = [...state.nodes];

            for (var i = 0; i < nodes.length; i++) {
                if(nodes[i].id == action.parentNode.id) {
                    nodes[i] = node(nodes[i], action);
                    changed = true;
                }
            }
            if (changed) {
                return {...state, nodes}
            }
            return state;
        case types.CHANGE_ADD_LEVEL:

            var changed = false;
            var nodes = [...state.nodes];

            for (var i = 0; i < nodes.length; i++) {
                if(nodes[i].id == action.parentNodeId) {
                    nodes[i] = node(nodes[i], action);
                    changed = true;
                }
            }
            if (changed) {
                return {...state, nodes}
            }
            return state;
        case types.CHANGE_FUND_RECORD:
            var index = indexById(state.nodes, action.nodeId, 'selectedSubNodeId');
            if (index !== null) {
                var nodes = [...state.nodes];
                nodes[index] = node(nodes[index], action);
                return {...state, nodes};
            }
            return state;
        default:
            return state
    }
}
