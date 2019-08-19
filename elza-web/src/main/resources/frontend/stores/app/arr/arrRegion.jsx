 import * as types from 'actions/constants/ActionTypes.js';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import {fund, fundInitState} from './fund.jsx'
import nodeSetting from './nodeSetting.jsx'
import fundSearch from './fundSearch.jsx'
import visiblePolicy from './visiblePolicy.jsx'
import {consolidateState} from 'components/Utils.jsx'
import {isBulkAction} from 'actions/arr/bulkActions.jsx'
import {isFundTreeAction} from 'actions/arr/fundTree.jsx'
import {isFundSearchAction} from '../../../actions/arr/fundSearch.jsx'
import {nodeFormActions, outputFormActions, structureFormActions} from 'actions/arr/subNodeForm.jsx'
import {isSubNodeDaosAction} from 'actions/arr/subNodeDaos.jsx'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx'
import {isNodeInfoAction} from 'actions/arr/nodeInfo.jsx'
import {isVersionValidation} from 'actions/arr/versionValidation.jsx'
import {isNodeAction} from 'actions/arr/node.jsx'
import {isNodesAction} from 'actions/arr/nodes.jsx'
import {isDeveloperScenariosAction} from 'actions/global/developer.jsx'
import {isNodeSettingsAction} from 'actions/arr/nodeSetting.jsx'
import {isFundDataGridAction} from 'actions/arr/fundDataGrid.jsx'
import {isFundChangeAction} from 'actions/global/change.jsx'
import {isFundFilesAction} from 'actions/arr/fundFiles.jsx'
import {isFundActionAction} from 'actions/arr/fundAction.jsx'
import {isFundOutput} from 'actions/arr/fundOutput.jsx'
import {isFundOutputFilesAction} from 'actions/arr/fundOutputFiles.jsx'
import processAreaStores from "shared/utils/processAreaStores";
import isCommonArea from "stores/utils/isCommonArea";
import globalFundTree from "./globalFundTree";
import {isStructureNodeForm} from "../../../actions/arr/structureNodeForm";
 import fundTree from "./fundTree";

 const initialState = {
    activeIndex: null,
    nodeSettings: nodeSetting(undefined, {}),
    extendedView: false,
    showRegisterJp: false,
    visiblePolicy: visiblePolicy(),
    funds: [],
    customFund: customFund(),
    globalFundTree: globalFundTree(undefined, {}),
    fundSearch: fundSearch(undefined, {})
};

 const initialCustomFundState = {
     fundTreeNodes: fundTree(),
     versionId: null,
     fundId: null,
 };

 function customFund(state = initialCustomFundState, action = {}) {
     if (isFundTreeAction(action)) {
         return {
             ...state,
             fundTreeNodes: fundTree(state.fundTreeNodes, action)
         }
     }

     switch (action.type) {
         case types.CUSTOM_FUND_ACTION_SELECT_VERSION: {
             return {
                 ...state,
                 versionId: action.version.id,
                 fundId: action.fundId,
                 customFund: customFund(),
             }
         }
         default:
             return {
                 ...state,
                 fundTreeNodes: fundTree(),
             }
     }
 }

function selectFundTab(state, action) {
    return {
        ...state,
        funds: [
            fundInitState(action.fund)
        ],
        activeIndex: 0
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
    if (isCommonArea(action.area)) {
        if (action.area.startsWith("fund[")) { // area pro zpracování na předaný fund, ten zde můžeme zpracovat
            return processAreaStores(state, action);
        }
    }

    if (isFundSearchAction(action)) {
        return {
            ...state,
            fundSearch: fundSearch(state.fundSearch, action)
        }
    }

    if (isBulkAction(action)
        || (isFundTreeAction(action) && (action.area !== types.FUND_TREE_AREA_COPY && action.area !== types.FUND_TREE_AREA_USAGE && action.area !== types.CUSTOM_FUND_TREE_AREA_NODES))
        || nodeFormActions.isSubNodeFormAction(action) || outputFormActions.isSubNodeFormAction(action) ||  structureFormActions.isSubNodeFormAction(action)
        || nodeFormActions.isSubNodeFormCacheAction(action) || outputFormActions.isSubNodeFormCacheAction(action) || structureFormActions.isSubNodeFormCacheAction(action)
        || isSubNodeDaosAction(action)
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
        || isVersionValidation(action)
        || isNodeAction(action)
        || isNodesAction(action)
        || isDeveloperScenariosAction(action)
        || isFundDataGridAction(action)
        || isFundChangeAction(action)
        || isFundFilesAction(action)
        || isFundActionAction(action)
        || isFundOutput(action)
        || isStructureNodeForm(action)
    ) {
        const index = indexById(state.funds, action.versionId, "versionId");
        if (index !== null) {
            return processFund(state, action, index)
        } else {
            return state
        }
    }

    if (isFundTreeAction(action) && (action.area === types.FUND_TREE_AREA_USAGE || action.area === types.FUND_TREE_AREA_COPY)) {
        return {
            ...state,
            globalFundTree: globalFundTree(state.globalFundTree, action)
        }
    }

    if (isFundTreeAction(action) && action.area === types.CUSTOM_FUND_TREE_AREA_NODES) {
        return {
            ...state,
            customFund: customFund(state.customFund, action),
        }
    }

    if (isNodeSettingsAction(action)) {
        var result =  {
            ...state,
            nodeSettings: nodeSetting(state.nodeSettings, action)
        };
        return consolidateState(state, result);
    }

    switch (action.type) {

        //case types.LOGOUT:
        case types.LOGIN_SUCCESS: {
            if (action.reset) {
                return initialState;
            }
            return state;
        }

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
        case types.CUSTOM_FUND_ACTION_SELECT_VERSION: {
            return {
                ...state,
                customFund: customFund(state.customFund, action),
            }
        }
        case types.STORE_LOAD:
            if (action.arrRegion) {
                return {
                    ...state,
                    ...action.arrRegion,
                    funds: action.arrRegion.funds.map(fundobj => fund(fundobj, action)),
                    extendedView: false
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
                        ],
                        extendedView: false
                    }
                } else {    // přidáme novou
                    return {
                        ...state,
                        activeIndex: state.funds.length,
                        funds: [
                            ...state.funds,
                            fund(action.arrRegionFund, action),
                        ],
                        extendedView: false
                    }
                }
            } else {
                return state;
            }
        case types.STORE_SAVE:
            const {activeIndex, nodeSettings} = state;
            return {
                activeIndex,
                nodeSettings,
                funds: state.funds.map(fundobj => fund(fundobj, action))
            }
        case types.FUND_EXTENDED_VIEW:
            var result = {...state, extendedView: action.enable}
            return consolidateState(state, result);
        case types.FUND_NODE_INCREASE_VERSION:
        case types.FUND_NODE_CHANGE:
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST:
        case types.FUND_FUND_CHANGE_READ_MODE:
        case types.FUND_TEMPLATE_USE:
        case types.CHANGE_NODES:
        case types.FUND_SUBNODE_UPDATE:
        case types.CHANGE_OUTPUTS:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_MOVE_LEVEL:
        case types.FUND_NODES_MOVE_START:
        case types.FUND_NODES_MOVE_STOP:
        case types.FUND_FUND_APPROVE_VERSION:
        case types.CHANGE_FUND_RECORD:
        case types.FUND_FUND_SELECT_SUBNODE:
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_CHANGES_DETAIL:
        case types.OUTPUT_INCREASE_VERSION:
        case types.OUTPUT_STATE_CHANGE:
        case types.NODES_DELETE:
        case types.CHANGE_FUND_ACTION:
            var index = indexById(state.funds, action.versionId, "versionId");
            return processFund(state, action, index);

        case types.FUND_FUNDS_REQUEST:
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
        case types.CHANGE_FILES:{
            const result = {
                ...state
            };

            let someFundChanged = false;
            const funds = state.funds.map(fundObj => {
                if (fundObj.id === action.fundId) {
                    someFundChanged = true;
                    return fund(fundObj, action)
                } else {
                    return fundObj
                }
            });
            if (someFundChanged) {
                result.funds = funds
            }

            return result;
        }
        case types.CHANGE_CONFORMITY_INFO:
        case types.CHANGE_NODE_REQUESTS:
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

        case types.FUND_INVALID:
            let result = {...state};
            action.fundVersionIds.forEach(fundVersionId => {
                var index = indexById(state.funds, fundVersionId, "versionId");
                result = processFund(result, action, index)
            });
            return consolidateState(state, result);

        default:
            return state
    }
}

