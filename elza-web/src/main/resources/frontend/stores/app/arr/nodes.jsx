import * as types from 'actions/constants/actionTypes';
import {indexById, findByNodeKeyInNodes, selectedAfterClose} from 'stores/app/utils.jsx'
import {node, nodeInitState} from './node.jsx'
import {consolidateState} from 'components/Utils'

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
        case types.FA_NODES_RECEIVE:
        case types.FA_NODES_REQUEST:
        case types.CHANGE_DESC_ITEM:
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
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
        case types.FA_SUB_NODE_REGISTER_REQUEST:
        case types.FA_SUB_NODE_REGISTER_RECEIVE:
        case types.FA_SUB_NODE_REGISTER_VALUE_RESPONSE:
        case types.FA_SUB_NODE_REGISTER_VALUE_DELETE:
        case types.FA_SUB_NODE_REGISTER_VALUE_ADD:
        case types.FA_SUB_NODE_REGISTER_VALUE_CHANGE:
        case types.FA_SUB_NODE_REGISTER_VALUE_FOCUS:
        case types.FA_SUB_NODE_REGISTER_VALUE_BLUR:
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
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
        case types.FA_FA_SUBNODES_FULLTEXT_RESULT:
            var r = findByNodeKeyInNodes(state, action.versionId, action.nodeKey);
            if (r) {
                var index = r.nodeIndex;
                return processNode(state, action, index);
            } else {
                return state;
            }
        case types.FA_FA_SUBNODES_NEXT:
        case types.FA_FA_SUBNODES_PREV:
        case types.FA_FA_SUBNODES_NEXT_PAGE:
        case types.FA_FA_SUBNODES_PREV_PAGE:
        case types.FA_FA_SUBNODES_FULLTEXT_SEARCH:
            var index = state.activeIndex;
            return processNode(state, action, index);
        case types.FA_FA_SELECT_SUBNODE:
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
        case types.FA_FA_CLOSE_NODE_TAB:
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
        case types.FA_FA_SELECT_NODE_TAB:
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

            for (var i = 0; i < nodes.length; i++) {
                var index = indexById(nodes[i].childNodes, action.nodeId);

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

            if (changed) {
                return {
                    ...state,
                    nodes: nodesChange,
                }
            }

            return state;
        case types.FA_NODE_CHANGE:
            var newState = Object.assign({}, state);
            for (var i = 0; i < newState.nodes.length; i++) {
                if(newState.nodes[i].id == action.parentNode.id) {
                    newState.nodes[i] = node(newState.nodes[i], action);
                }
            }
            return newState;
        case types.CHANGE_ADD_LEVEL:
            var newState = Object.assign({}, state);
            for (var i = 0; i < newState.nodes.length; i++) {
                if(newState.nodes[i].id == action.parentNodeId) {
                    newState.nodes[i] = node(newState.nodes[i], action);
                }
            }
            return newState;
        default:
            return state
    }
}
