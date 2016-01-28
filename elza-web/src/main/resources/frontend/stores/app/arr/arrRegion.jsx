import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'
import {fa, faInitState} from './fa'
import faTree from './faTree'
import nodeSetting from './nodeSetting'

const initialState = {
    activeIndex: null,
    nodeSettings: nodeSetting(undefined, {}),
    extendedView: false,
    packets: {},
    fas: [],
}

function selectFaTab(state, action) {
    var index = indexById(state.fas, action.fa.id);
    if (index == null) {    // není zatím v seznamu, přidáme jí tam
        return {
            ...state,
            fas: [
                ...state.fas,
                faInitState(action.fa)
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

export default function arrRegion(state = initialState, action) {
    switch (action.type) {
        case types.STORE_LOAD:
            if (action.arrRegion) {
                return {
                    ...state,
                    packets: {},
                    ...action.arrRegion,
                    fas: action.arrRegion.fas.map(faobj => fa(faobj, action))
                }
            } else if (action.arrRegionFa) {
                var index = indexById(state.fas, action.arrRegionFa.versionId, "versionId");
                if (index !== null) {   // existuje, nahradí se
                    return {
                        ...state,
                        activeIndex: index,
                        fas: [
                            ...state.fas.slice(0, index),
                            fa(action.arrRegionFa, action),
                            ...state.fas.slice(index + 1)
                        ]
                    }
                } else {    // přidáme novou
                    return {
                        ...state,
                        activeIndex: state.fas.length,
                        fas: [
                            ...state.fas,
                            fa(action.arrRegionFa, action),
                        ]
                    }
                }
            } else {
                return state;
            }
        case types.STORE_SAVE:
            const {activeIndex, nodeSettings, extendedView} = state;
            return {
                activeIndex,
                nodeSettings,
                extendedView,
                fas: state.fas.map(faobj => fa(faobj, action))
            }
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
        case types.FA_FA_TREE_FULLTEXT_RESULT:
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
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
        case types.FA_NODE_CHANGE:
            var index = indexById(state.fas, action.versionId, "versionId");
            if (index != null) {
                return {
                    ...state,
                    fas: [
                        ...state.fas.slice(0, index),
                        fa(state.fas[index], action),
                        ...state.fas.slice(index + 1)
                    ]
                }
            } else {
                return state;
            }
        case types.FA_FA_TREE_FULLTEXT_CHANGE:
        case types.FA_FA_TREE_FOCUS_NODE:
        case types.FA_FA_TREE_EXPAND_NODE:
        case types.FA_FA_TREE_COLLAPSE_NODE:
        case types.FA_FA_TREE_SELECT_NODE:
        case types.GLOBAL_CONTEXT_MENU_HIDE:
        case types.FA_FA_SELECT_SUBNODE:
        case types.FA_FA_SUBNODES_NEXT:
        case types.FA_FA_SUBNODES_PREV:
        case types.FA_FA_SUBNODES_NEXT_PAGE:
        case types.FA_FA_SUBNODES_PREV_PAGE:
        case types.FA_FA_CLOSE_NODE_TAB:
        case types.FA_FA_SELECT_NODE_TAB:
            var index = state.activeIndex;
            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, index),
                    fa(state.fas[index], action),
                    ...state.fas.slice(index + 1)
                ]
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
                        dirty: false,
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

        case types.CHANGE_PACKETS:

            var packets = state.packets;
            var faPackets = packets[action.findingAidId];

            if (faPackets == null) {
                return state;
            } else {
                faPackets.dirty = true;
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
            faPackets.dirty = false;
            faPackets.items = action.items;

            packets[action.findingAidId] = faPackets;

            return {
                ...state,
                packets
            }

        case types.CREATE_PACKET_RECEIVE:

            var packets = Object.assign({}, state.packets);

            packets[action.findingAidId].items.push(action.data);

            return {
                ...state,
                packets: packets
            }

        case types.CHANGE_CONFORMITY_INFO:
            var index = indexById(state.fas, action.findingAidVersionId);

            // změna se ho netýká, vracím původní stav
            if (index == null) {
                return state;
            }

            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, index),
                    fa(state.fas[index], action),
                    ...state.fas.slice(index + 1)
                ]
            }            
        default:
            return state
    }
}

