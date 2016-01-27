import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'
import faTree from './faTree'
import nodeSetting from './nodeSetting'

const initialState = {
    activeIndex: null,
    nodeSettings: undefined,
    extendedView: false,
    packets: {},
    fas: [],
}

function selectFaTab(state, action) {
    var index = indexById(state.fas, action.fa.id);
    if (index == null) {    // není zatím v seznamu, přidáme jí tam
        var faItem = Object.assign({}, action.fa, {
            faTree: {...faTree(action.fa.faTree, action)},
            faTreeMovementsLeft: {...faTree(action.fa.faTreeMovementsLeft, action)},
            faTreeMovementsRight: {...faTree(action.fa.faTreeMovementsRight, action)},
            nodes: nodes(undefined, action)
        });

        faItem.faTreeMovementsLeft.multipleSelection = true;
        faItem.faTreeMovementsLeft.multipleSelectionOneLevel = true;

        return {
            ...state,
            fas: [
                ...state.fas,
                faItem
            ],
            activeIndex: state.fas.length
        }
    } else {
        return {
            ...state,
            activeIndex: index
        }
    }
}

function updateFaTree(fa, action) {
    switch (action.area) {
        case types.FA_TREE_AREA_MAIN:
            fa.faTree = faTree(fa.faTree, action)
            break;
        case types.FA_TREE_AREA_MOVEMENTS_LEFT:
            fa.faTreeMovementsLeft = faTree(fa.faTreeMovementsLeft, action)
            break;
        case types.FA_TREE_AREA_MOVEMENTS_RIGHT:
            fa.faTreeMovementsRight = faTree(fa.faTreeMovementsRight, action)
            break;
    }
}

export default function arrRegion(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_GET_OBJECT_INFO:
            state.fas.forEach(fa => {
                action.objectInfo.addFa(fa);
                nodes(fa.nodes, action);
            });
            return state
        case types.FA_EXTENDED_VIEW:
            return {...state, extendedView: action.enable}
        case types.FA_FA_TREE_REQUEST:
        case types.FA_FA_TREE_RECEIVE:
            var index = indexById(state.fas, action.versionId, "versionId");
            if (index != null) {
                var newFa = Object.assign({}, state.fas[index]);
                updateFaTree(newFa, action);

                return {
                    ...state,
                    fas: [
                        ...state.fas.slice(0, index),
                        newFa,
                        ...state.fas.slice(index + 1)
                    ]
                }
            } else {
                return state;
            }
        case types.FA_FA_TREE_FULLTEXT_RESULT:
            var index = indexById(state.fas, action.versionId, "versionId");
            if (index != null) {
                var newFa = Object.assign({}, state.fas[index]);
                updateFaTree(newFa, action);

                return {
                    ...state,
                    fas: [
                        ...state.fas.slice(0, index),
                        newFa,
                        ...state.fas.slice(index + 1)
                    ]
                }
            } else {
                return result;
            }
        case types.FA_FA_TREE_FULLTEXT_CHANGE:
        case types.FA_FA_TREE_FOCUS_NODE:
        case types.FA_FA_TREE_EXPAND_NODE:
        case types.FA_FA_TREE_COLLAPSE_NODE:
        case types.FA_FA_TREE_SELECT_NODE:
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            var newFa = Object.assign({}, state.fas[state.activeIndex]);
            updateFaTree(newFa, action);

            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, state.activeIndex),
                    newFa,
                    ...state.fas.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_FA_SELECT_SUBNODE:
            var newFa = Object.assign({}, state.fas[state.activeIndex]);
            updateFaTree(newFa, action);
            newFa.nodes = nodes(state.fas[state.activeIndex].nodes, action);

            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, state.activeIndex),
                    newFa,
                    ...state.fas.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_FA_SUBNODES_NEXT:
        case types.FA_FA_SUBNODES_PREV:
        case types.FA_FA_SUBNODES_NEXT_PAGE:
        case types.FA_FA_SUBNODES_PREV_PAGE:
        case types.FA_FA_CLOSE_NODE_TAB:
        case types.FA_FA_SELECT_NODE_TAB:
            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, state.activeIndex),
                    Object.assign({}, state.fas[state.activeIndex], {nodes: nodes(state.fas[state.activeIndex].nodes, action)}),
                    ...state.fas.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
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
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
            var faIndex = indexById(state.fas, action.versionId, "versionId");
            if (faIndex != null) {
                return {
                    ...state,
                    fas: [
                        ...state.fas.slice(0, faIndex),
                        Object.assign({}, state.fas[faIndex], {nodes: nodes(state.fas[faIndex].nodes, action)}),
                        ...state.fas.slice(faIndex + 1)
                    ]
                }
            } else {
                return state;
            }
        case types.FA_CLOSE_FA_TAB:
            var index = indexById(state.fas, action.fa.id);
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.fas, index);
            } else if (index < state.activeIndex) {
                newActiveIndex--;
            }
            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, index),
                    ...state.fas.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case types.FA_SELECT_FA_TAB:
            return selectFaTab(state, action);

        case types.NODE_DESC_ITEM_TYPE_LOCK:
        case types.NODE_DESC_ITEM_TYPE_UNLOCK:
        case types.NODE_DESC_ITEM_TYPE_UNLOCK_ALL:
        case types.NODE_DESC_ITEM_TYPE_COPY:
        case types.NODE_DESC_ITEM_TYPE_NOCOPY:
            return {
                ...state,
                nodeSettings: nodeSetting(state.nodeSettings, action)
            }

        case types.PACKETS_REQUEST:

            var packets = state.packets;
            var faPackets = packets[action.findingAidId];

            if (faPackets == null) {
                faPackets = {
                        isFetching: true,
                        fetched: false,
                        items: []
                }
            } else {
                faPackets.isFetching = true
            }


            packets[action.findingAidId] = faPackets;

            return {
                ...state,
                packets
            }

        case types.PACKETS_RECEIVE:

            var packets = state.packets;
            var faPackets = packets[action.findingAidId];

            faPackets.isFetching = false;
            faPackets.fetched = true;
            faPackets.items = action.items;

            packets[action.findingAidId] = faPackets;

            return {
                ...state,
                packets
            }

        case types.CHANGE_CONFORMITY_INFO:

            var index = indexById(state.fas, action.findingAidVersionId);

            // změna se ho netýká, vracím původní stav
            if (index == null) {
                return state;
            }

            var faTreeChange = faTree(state.fas[index].faTree, action);
            var nodesChange = nodes(state.fas[index].nodes, action);

            // nezměnil se stav podřízených, nemusím nic měnit
            if (faTreeChange === state.fas[index].faTree && nodesChange === state.fas[index].nodes) {
                return state;
            }

            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, index),
                    Object.assign({}, state.fas[index], {faTree: faTreeChange, nodes: nodesChange}),
                    ...state.fas.slice(index + 1)
                ]
            }

        default:
            return state
    }
}

