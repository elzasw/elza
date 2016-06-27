 import * as types from 'actions/constants/ActionTypes.js';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes.jsx'
import {fund, fundInitState} from './fund.jsx'
import fundTree from './fundTree.jsx'
import nodeSetting from './nodeSetting.jsx'
import visiblePolicy from './visiblePolicy.jsx'
import {consolidateState} from 'components/Utils.jsx'
import {Toastr, i18n} from 'components/index.jsx';
import {isBulkAction} from 'actions/arr/bulkActions.jsx'
import {isFundTreeAction} from 'actions/arr/fundTree.jsx'
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {isSubNodeRegisterAction} from 'actions/arr/subNodeRegister.jsx'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx'
import {isNodeInfoAction} from 'actions/arr/nodeInfo.jsx'
import {isVersionValidation} from 'actions/arr/versionValidation.jsx'
import {isNodeAction} from 'actions/arr/node.jsx'
import {isNodesAction} from 'actions/arr/nodes.jsx'
import {isDeveloperScenariosAction} from 'actions/global/developer.jsx'
import {isNodeSettingsAction} from 'actions/arr/nodeSetting.jsx'
import {isFundDataGridAction} from 'actions/arr/fundDataGrid.jsx'
import {isFundChangeAction} from 'actions/global/change.jsx'
import {isFundPacketsAction} from 'actions/arr/fundPackets.jsx'
import {isFundActionAction} from 'actions/arr/fundAction.jsx'
import {isFundOutput} from 'actions/arr/fundOutput.jsx'

const initialState = {
    activeIndex: null,
    nodeSettings: nodeSetting(undefined, {}),
    extendedView: false,
    showRegisterJp: false,
    packets: {},
    visiblePolicy: visiblePolicy(),
    funds: [],
}

function selectFundTab(state, action) {
    var index = indexById(state.funds, action.fund.versionId, "versionId");
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
        || nodeFormActions.isSubNodeFormAction(action)
        || nodeFormActions.isSubNodeFormCacheAction(action)
        || isSubNodeRegisterAction(action)
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
        || isVersionValidation(action)
        || isNodeAction(action)
        || isNodesAction(action)
        || isDeveloperScenariosAction(action)
        || isFundDataGridAction(action)
        || isFundChangeAction(action)
        || isFundPacketsAction(action)
        || isFundActionAction(action)
        || isFundOutput(action)
    ) {
        var index = indexById(state.funds, action.versionId, "versionId")
        if (index !== null) {
            return processFund(state, action, index)
        } else {
            return state
        }
    }

    if (isNodeSettingsAction(action)) {
        var result =  {
            ...state,
            nodeSettings: nodeSetting(state.nodeSettings, action)
        }
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.DELETE_FUND: {
            const newFunds = [];
            state.funds.map(function(item) {
                if (item.id != action.fundId) {
                    newFunds.push(item);
                }
            });

            var newIndex = initialState.activeIndex;
            if (state.activeIndex !== null && state.funds[state.activeIndex].id != action.fundId) {
                newIndex = indexById(newFunds, state.funds[state.activeIndex].versionId, 'versionId');
            }

            return {
                ...state,
                funds: newFunds,
                activeIndex: newIndex
            }
        }
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
        case types.FUND_NODE_CHANGE:
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST:
        case types.CHANGE_NODES:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_MOVE_LEVEL:
        case types.FUND_FUND_APPROVE_VERSION:
        case types.CHANGE_FUND_RECORD:
        case types.FUND_FUND_SELECT_SUBNODE:
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_CHANGES_DETAIL:
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
        case types.CHANGE_FUND:

            var i = 0;
            state.funds.forEach(fund => {
                if (fund.id = action.fundId) {
                    state = processFund(state, action, i++);
                }
            });

            return state;
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            var index = state.activeIndex;
            return processFund(state, action, index);
        case types.FUND_CLOSE_FUND_TAB:
            var index = indexById(state.funds, action.fund.versionId, "versionId");
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

            var result = {
                ...state,
                packets
            }

            var someFundChanged = false
            var funds = state.funds.map(fundObj => {
                if (fundObj.id === action.fundId) {
                    someFundChanged = true
                    return fund(fundObj, action)
                } else {
                    return fundObj
                }
            })
            if (someFundChanged) {
                result.funds = funds
            }

            return result
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
        case types.CHANGE_CONFORMITY_INFO:
            var index = indexById(state.funds, action.fundVersionId, "versionId");

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

            funds.forEach(fund => {if (fund.versionId == action.versionId) {
                if (fund.closed == false) {
                    update = true;
                    fund.closed = true;
                }
            }});

            if (update) {
                return {...state}
            }

            return state

        case types.VISIBLE_POLICY_REQUEST:
        case types.VISIBLE_POLICY_RECEIVE:
        case types.SET_VISIBLE_POLICY_REQUEST:
        case types.SET_VISIBLE_POLICY_RECEIVE:
            return {
                ...state,
                visiblePolicy: visiblePolicy(state.visiblePolicy, action),
            }

        case types.FUND_FUND_NODES_POLICY_RECEIVE:
        case types.FUND_FUND_NODES_POLICY_REQUEST:
            var index = indexById(state.funds, action.versionId, "versionId");
            return processFund(state, action, index);

        default:
            return state
    }
}

