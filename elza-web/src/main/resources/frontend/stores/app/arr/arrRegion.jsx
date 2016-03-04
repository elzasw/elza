import * as types from 'actions/constants/ActionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'
import {fa, faInitState} from './fa'
import faTree from './faTree'
import nodeSetting from './nodeSetting'
import {consolidateState} from 'components/Utils'
import {Toastr, i18n} from 'components';

const initialState = {
    activeIndex: null,
    nodeSettings: nodeSetting(undefined, {}),
    extendedView: false,
    showRegisterJp: false,
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
    } else if (index !== state.activeIndex) {
        return {
            ...state,
            activeIndex: index
        }
    } else {
        return state;
    }
}

function processFa(state, action, index) {
    if (index != null) {
        var newFa = fa(state.fas[index], action);
        if (newFa !== state.fas[index]) {
            var result = {
                ...state,
                fas: [
                    ...state.fas.slice(0, index),
                    newFa,
                    ...state.fas.slice(index + 1)
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

export default function arrRegion(state = initialState, action) {
    switch (action.type) {

        case types.SHOW_REGISTER_JP: {
            return {
                ...state,
                showRegisterJp: action.showRegisterJp
            }
        }

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
        case types.FA_EXTENDED_VIEW:
            var result = {...state, extendedView: action.enable}
            return consolidateState(state, result);
        case types.FA_FA_TREE_REQUEST:
        case types.FA_FA_TREE_RECEIVE:
        case types.FA_FA_TREE_FULLTEXT_RESULT:
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_FORM_CACHE_RESPONSE:
        case types.FA_SUB_NODE_FORM_CACHE_REQUEST:
        case types.FA_SUB_NODE_REGISTER_REQUEST:
        case types.FA_SUB_NODE_REGISTER_RECEIVE:
        case types.FA_SUB_NODE_REGISTER_VALUE_RESPONSE:
        case types.FA_SUB_NODE_REGISTER_VALUE_DELETE:
        case types.FA_SUB_NODE_REGISTER_VALUE_ADD:
        case types.FA_SUB_NODE_REGISTER_VALUE_CHANGE:
        case types.FA_SUB_NODE_REGISTER_VALUE_FOCUS:
        case types.FA_SUB_NODE_REGISTER_VALUE_BLUR:
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
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
        case types.FA_FA_SUBNODES_FULLTEXT_RESULT:
        case types.FA_NODE_CHANGE:
        case types.FA_NODES_RECEIVE:
        case types.FA_NODES_REQUEST:
        case types.CHANGE_NODES:
        case types.BULK_ACTIONS_STATE_CHANGE:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_MOVE_LEVEL:
        case types.FA_VERSION_VALIDATION_LOAD:
        case types.FA_VERSION_VALIDATION_RECEIVED:
        case types.FA_FA_APPROVE_VERSION:
        case types.CHANGE_FA:
        case types.CHANGE_FA_RECORD:
        case types.DEVELOPER_SCENARIOS_RECEIVED:
        case types.DEVELOPER_SCENARIOS_FETCHING:
        case types.DEVELOPER_SCENARIOS_DIRTY:
            var index = indexById(state.fas, action.versionId, "versionId");
            return processFa(state, action, index);
        case types.FA_FAS_RECEIVE:
            var changed = false;
            var newFas = state.fas.map(faObj => {
                if (action.faMap[faObj.versionId]) {
                    var newFa = fa(faObj, action);
                    if (faObj !== newFa) {
                        changed = true;
                    }
                    return newFa;
                } else {
                    return faObj;
                }
            })
            if (changed) {
                return {
                    ...state,
                    fas: newFas
                }
            } else {
                return state
            }
        case types.FA_FA_TREE_FULLTEXT_CHANGE:
        case types.FA_FA_TREE_FOCUS_NODE:
        case types.FA_FA_TREE_EXPAND_NODE:
        case types.FA_FA_TREE_COLLAPSE:
        case types.FA_FA_TREE_COLLAPSE_NODE:
        case types.FA_FA_TREE_SELECT_NODE:
        case types.GLOBAL_CONTEXT_MENU_HIDE:
        case types.FA_FA_SELECT_SUBNODE:
        case types.FA_FA_SUBNODES_NEXT:
        case types.FA_FA_SUBNODES_PREV:
        case types.FA_FA_SUBNODES_NEXT_PAGE:
        case types.FA_FA_SUBNODES_PREV_PAGE:
        case types.FA_FA_SUBNODES_FULLTEXT_SEARCH:
        case types.FA_FA_CLOSE_NODE_TAB:
        case types.FA_FA_SELECT_NODE_TAB:
        case types.BULK_ACTIONS_DATA_LOADING:
        case types.BULK_ACTIONS_DATA_LOADED:
        case types.BULK_ACTIONS_RECEIVED_DATA:
        case types.BULK_ACTIONS_VERSION_VALIDATE_RECEIVED_DATA:
        case types.BULK_ACTIONS_RECEIVED_ACTIONS:
        case types.BULK_ACTIONS_RECEIVED_STATES:
        case types.BULK_ACTIONS_RECEIVED_STATE:
        case types.BULK_ACTIONS_STATE_IS_DIRTY:
            var index = state.activeIndex;
            return processFa(state, action, index);
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
            var result =  {
                ...state,
                nodeSettings: nodeSetting(state.nodeSettings, action)
            }
            return consolidateState(state, result);
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

        case types.CHANGE_APPROVE_VERSION:

            var fas = state.fas;
            var update = false;

            fas.forEach(fa => {if (fa.id == action.versionId) {
                if (fa.closed == false) {
                    update = true;
                    fa.closed = true;
                }
            }});

            if (update) {
                return {...state}
            }

            return state

        default:
            return state
    }
}

