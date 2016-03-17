import * as types from 'actions/constants/ActionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'
import {fund, fundInitState} from './fund'
import fundTree from './fundTree'
import nodeSetting from './nodeSetting'
import {consolidateState} from 'components/Utils'
import {Toastr, i18n} from 'components';
import {isBulkAction} from 'actions/arr/bulkActions'
import {isFundTreeAction} from 'actions/arr/fundTree'
import {isSubNodeFormAction, isSubNodeFormCacheAction} from 'actions/arr/subNodeForm'
import {isSubNodeRegisterAction} from 'actions/arr/subNodeRegister'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo'
import {isNodeInfoAction} from 'actions/arr/nodeInfo'
import {isVersionValidation} from 'actions/arr/versionValidation'

const initialState = {
    activeIndex: null,
    nodeSettings: nodeSetting(undefined, {}),
    extendedView: false,
    showRegisterJp: false,
    packets: {},
    funds: [],
}

function selectFundTab(state, action) {
    var index = indexById(state.funds, action.fund.id);
    if (index == null) {    // není zatím v seznamu, přidáme jí tam
        return {
            ...state,
            funds: [
                ...state.funds,
                fundInitState(action.fund)
            ],
            activeIndex: state.funds.length
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

function processFund(state, action, index) {
    if (index != null) {
        var newFund = fund(state.funds[index], action);
        if (newFund !== state.funds[index]) {
            var result = {
                ...state,
                funds: [
                    ...state.funds.slice(0, index),
                    newFund,
                    ...state.funds.slice(index + 1)
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
    if (false
        || isBulkAction(action)
        || isFundTreeAction(action)
        || isSubNodeFormAction(action)
        || isSubNodeFormCacheAction(action)
        || isSubNodeRegisterAction(action)
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
        || isVersionValidation(action)
    ) {
        var index = indexById(state.funds, action.versionId, "versionId");
        return processFund(state, action, index);
    }

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
                    funds: action.arrRegion.funds.map(fundobj => fund(fundobj, action))
                }
            } else if (action.arrRegionFund) {
                var index = indexById(state.funds, action.arrRegionFund.versionId, "versionId");
                if (index !== null) {   // existuje, nahradí se
                    return {
                        ...state,
                        activeIndex: index,
                        funds: [
                            ...state.funds.slice(0, index),
                            fund(action.arrRegionFund, action),
                            ...state.funds.slice(index + 1)
                        ]
                    }
                } else {    // přidáme novou
                    return {
                        ...state,
                        activeIndex: state.funds.length,
                        funds: [
                            ...state.funds,
                            fund(action.arrRegionFund, action),
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
                funds: state.funds.map(fundobj => fund(fundobj, action))
            }
        case types.FUND_EXTENDED_VIEW:
            var result = {...state, extendedView: action.enable}
            return consolidateState(state, result);
        case types.FUND_FUND_SUBNODES_FULLTEXT_RESULT:
        case types.FUND_NODE_CHANGE:
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST:
        case types.CHANGE_NODES:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_MOVE_LEVEL:
        case types.FUND_FUND_APPROVE_VERSION:
        case types.CHANGE_FUND:
        case types.CHANGE_FUND_RECORD:
        case types.DEVELOPER_SCENARIOS_RECEIVED:
        case types.DEVELOPER_SCENARIOS_FETCHING:
        case types.DEVELOPER_SCENARIOS_DIRTY:
        case types.FUND_FUND_SELECT_SUBNODE:
            var index = indexById(state.funds, action.versionId, "versionId");
            return processFund(state, action, index);
        case types.FUND_FUNDS_RECEIVE:
            var changed = false;
            var newFunds = state.funds.map(fundObj => {
                if (action.fundMap[fundObj.versionId]) {
                    var newFund = fund(fundObj, action);
                    if (fundObj !== newFund) {
                        changed = true;
                    }
                    return newFund;
                } else {
                    return fundObj;
                }
            })
            if (changed) {
                return {
                    ...state,
                    funds: newFunds
                }
            } else {
                return state
            }
        case types.GLOBAL_CONTEXT_MENU_HIDE:
        case types.FUND_FUND_SUBNODES_NEXT:
        case types.FUND_FUND_SUBNODES_PREV:
        case types.FUND_FUND_SUBNODES_NEXT_PAGE:
        case types.FUND_FUND_SUBNODES_PREV_PAGE:
        case types.FUND_FUND_SUBNODES_FULLTEXT_SEARCH:
        case types.FUND_FUND_CLOSE_NODE_TAB:
        case types.FUND_FUND_SELECT_NODE_TAB:
            var index = state.activeIndex;
            return processFund(state, action, index);
        case types.FUND_CLOSE_FUND_TAB:
            var index = indexById(state.funds, action.fund.id);
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.funds, index);
            } else if (index < state.activeIndex) {
                newActiveIndex--;
            }
            return {
                ...state,
                funds: [
                    ...state.funds.slice(0, index),
                    ...state.funds.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case types.FUND_SELECT_FUND_TAB:
            return selectFundTab(state, action);
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
            var fundPackets = packets[action.fundId];

            if (fundPackets == null) {
                fundPackets = {
                        isFetching: true,
                        fetched: false,
                        dirty: false,
                        items: []
                }
            } else {
                fundPackets.isFetching = true
            }

            packets[action.fundId] = fundPackets;

            return {
                ...state,
                packets
            }

        case types.CHANGE_PACKETS:
            var packets = state.packets;
            var fundPackets = packets[action.fundId];

            if (fundPackets == null) {
                return state;
            } else {
                fundPackets.dirty = true;
            }

            packets[action.fundId] = fundPackets;

            return {
                ...state,
                packets
            }
        case types.PACKETS_RECEIVE:
            var packets = state.packets;
            var fundPackets = packets[action.fundId];

            fundPackets.isFetching = false;
            fundPackets.fetched = true;
            fundPackets.dirty = false;
            fundPackets.items = action.items;

            packets[action.fundId] = fundPackets;

            return {
                ...state,
                packets
            }
        case types.CREATE_PACKET_RECEIVE:
            var packets = Object.assign({}, state.packets);

            packets[action.fundId].items.push(action.data);

            return {
                ...state,
                packets: packets
            }
        case types.CHANGE_CONFORMITY_INFO:
            var index = indexById(state.funds, action.fundVersionId);

            // změna se ho netýká, vracím původní stav
            if (index == null) {
                return state;
            }

            return {
                ...state,
                funds: [
                    ...state.funds.slice(0, index),
                    fund(state.funds[index], action),
                    ...state.funds.slice(index + 1)
                ]
            }

        case types.CHANGE_APPROVE_VERSION:

            var funds = state.funds;
            var update = false;

            funds.forEach(fund => {if (fund.id == action.versionId) {
                if (fund.closed == false) {
                    update = true;
                    fund.closed = true;
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

