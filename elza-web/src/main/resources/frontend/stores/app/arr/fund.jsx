import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import fundTree from './fundTree.jsx'
import nodes from './nodes.jsx'
import fundDataGrid from './fundDataGrid.jsx'
import bulkActions from './bulkActions.jsx'
import versionValidation from './versionValidation.jsx'
import fundNodesPolicy from './fundNodesPolicy.jsx'
import fundPackets from './fundPackets.jsx'
import fundOutput from './fundOutput.jsx'
import fundAction from './fundAction.jsx'
import {consolidateState} from 'components/Utils.jsx'
import {isBulkAction} from 'actions/arr/bulkActions.jsx'
import {isFundTreeAction} from 'actions/arr/fundTree.jsx'
import {isSubNodeFormAction, isSubNodeFormCacheAction} from 'actions/arr/subNodeForm.jsx'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx'
import {isNodeInfoAction} from 'actions/arr/nodeInfo.jsx'
import {isVersionValidation} from 'actions/arr/versionValidation.jsx'
import {isNodeAction} from 'actions/arr/node.jsx'
import {isNodesAction} from 'actions/arr/nodes.jsx'
import {isSubNodeRegisterAction} from 'actions/arr/subNodeRegister.jsx'
import {isDeveloperScenariosAction} from 'actions/global/developer.jsx'
import {isFundDataGridAction} from 'actions/arr/fundDataGrid.jsx'
import {isFundChangeAction} from 'actions/global/change.jsx'
import {isFundPacketsAction} from 'actions/arr/fundPackets.jsx'
import {isFundActionAction} from 'actions/arr/fundAction.jsx'
import {getNodeKeyType} from 'stores/app/utils.jsx'
import {isFundOutput} from 'actions/arr/fundOutput.jsx'

export function fundInitState(fundWithVersion) {
    var result = {
        ...fundWithVersion,
        id: fundWithVersion.id,
        closed: fundWithVersion.closed,
        versionId: fundWithVersion.versionId,
        name: fundWithVersion.name,
        isFetching: false,
        dirty: false,
        fundAction: fundAction(),
        fundOutput: fundOutput(),
        fundDataGrid: fundDataGrid(),
        fundPackets: fundPackets(),
        fundTree: fundTree(undefined, {type: ''}),
        fundTreeMovementsLeft: fundTree(undefined, {type: ''}),
        fundTreeMovementsRight: fundTree(undefined, {type: ''}),
        fundTreeNodes: fundTree(undefined, {type: ''}),
        nodes: nodes(undefined, {type: ''}),
        fundNodesPolicy: fundNodesPolicy(),
        bulkActions: bulkActions(undefined, {type: ''}),
        versionValidation: versionValidation(undefined, {type: ''})
    }

    result.fundTreeMovementsLeft = {...result.fundTreeMovementsLeft};
    result.fundTreeMovementsLeft.multipleSelection = true;
    result.fundTreeMovementsLeft.multipleSelectionOneLevel = true;

    result.fundTreeNodes = initFundTreeNodes(result.fundTreeNodes);

    return result;
}

function initFundTreeNodes(fundTreeNodes) {
    return {
        ...fundTreeNodes,
        multipleSelection: true,
        multipleSelectionOneLevel: false,
    };
}

function updateFundTree(state, action) {
    switch (action.area) {
        case types.FUND_TREE_AREA_MAIN:
            state.fundTree = fundTree(state.fundTree, action)
            break;
        case types.FUND_TREE_AREA_MOVEMENTS_LEFT:
            state.fundTreeMovementsLeft = fundTree(state.fundTreeMovementsLeft, action)
            break;
        case types.FUND_TREE_AREA_MOVEMENTS_RIGHT:
            state.fundTreeMovementsRight = fundTree(state.fundTreeMovementsRight, action)
            break;
        case types.FUND_TREE_AREA_NODES:
            state.fundTreeNodes = fundTree(state.fundTreeNodes, action)
            break;
    }
}

export function fund(state, action) {
    if (isBulkAction(action)) {
        var result = {...state, bulkActions: bulkActions(state.bulkActions, action)}
        return consolidateState(state, result);
    }
    
    if (isFundActionAction(action)) {
        var result = {...state, fundAction: fundAction(state.fundAction, action)}
        return consolidateState(state, result);
    }

    if (isFundOutput(action)) {
        var result = {...state, fundOutput: fundOutput(state.fundOutput, action)}
        return consolidateState(state, result);
    }

    if (isFundPacketsAction(action)) {
        var result = {...state, fundPackets: fundPackets(state.fundPackets, action)}
        return consolidateState(state, result);
    }

    if (isFundTreeAction(action)) {
        var result = {...state};
        updateFundTree(result, action);
        return consolidateState(state, result);
    }

    if (isVersionValidation(action)) {
        var result = {...state, versionValidation: versionValidation(state.versionValidation, action)};
        return consolidateState(state, result);
    }

    if (isFundDataGridAction(action)) {
        var result = {...state, fundDataGrid: fundDataGrid(state.fundDataGrid, action)};
        return consolidateState(state, result)
    }

    if (isSubNodeFormAction(action)) {
        const type = getNodeKeyType(action.nodeKey)
        switch (type) {
            case 'NODE':
                var result = {...state,
                    nodes: nodes(state.nodes, action),
                    fundTree: fundTree(state.fundTree, action),
                }
                return consolidateState(state, result);
            case 'DATA_GRID':
                var result = {...state, fundDataGrid: fundDataGrid(state.fundDataGrid, action)};
                return consolidateState(state, result)
        }
    }

    if (false
        || isSubNodeFormCacheAction(action)
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
        || isNodeAction(action)
        || isNodesAction(action)
        || isSubNodeRegisterAction(action)
        || isDeveloperScenariosAction(action)
    ) {
        var result = {...state,
            nodes: nodes(state.nodes, action),
            fundTree: fundTree(state.fundTree, action),
        }
        return consolidateState(state, result);
    }

    if (false || isFundChangeAction(action)) {
        var result = {...state,
            nodes: nodes(state.nodes, action),
            fundTree: fundTree(state.fundTree, action),
            fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
        }
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetching: false,
                closed: true,   // při načtení vždy chceme closed, i když není - aby nemohl editovat, než se načte aktuální stav ze serveru
                dirty: true,
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                fundTreeNodes: initFundTreeNodes(fundTree()),
                nodes: nodes(state.nodes, action),
                fundOutput: fundOutput(state.fundOutput, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
                fundPackets: fundPackets(state.fundPackets, action),
                fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
                bulkActions: bulkActions(undefined, {type: ''}),
                fundAction: fundAction(undefined, {type: ''}),
                versionValidation: versionValidation(undefined, {type: ''})
            }
        case types.STORE_SAVE:
            const {id, versionId, name, lockDate} = state;
            return {
                id,
                versionId,
                name,
                lockDate,
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                nodes: nodes(state.nodes, action),
                fundOutput: fundOutput(state.fundOutput, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
                fundPackets: fundPackets(state.fundPackets, action),
            }
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_CHANGES_DETAIL:
            var result = {
                ...state,
                fundOutput: fundOutput(state.fundOutput, action),
            }
            return consolidateState(state, result);
        case types.CHANGE_PACKETS:
            return {
                ...state,
                fundPackets: fundPackets(state.fundPackets, action)
            }
        case types.FUND_FUNDS_REQUEST:
            if (action.fundMap[state.versionId]) {
                return {
                    ...state,
                    isFetching: true,
                }
            } else {
                return state
            }
        case types.FUND_FUNDS_RECEIVE:
            if (action.fundMap[state.versionId]) {
                return {
                    ...state,
                    dirty: false,
                    isFetching: false,
                    ...action.fundMap[state.versionId],
                }
            } else {
                return state
            }
        case types.CHANGE_FUND:
            return {
                ...state,
                dirty: true,
            }
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            var result = {...state};
            updateFundTree(result, action);
            return consolidateState(state, result);
        case types.FUND_FUND_SELECT_SUBNODE:
            var result = {...state, nodes: nodes(state.nodes, action)}
            updateFundTree(result, action);
            return consolidateState(state, result);
        case types.FUND_NODE_CHANGE:
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST:
            var result = {...state,
                nodes: nodes(state.nodes, action),
                fundTree: fundTree(state.fundTree, action),
            }
            return consolidateState(state, result);
        case types.CHANGE_FUND_RECORD:
        case types.CHANGE_NODES:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_MOVE_LEVEL:
            var result = {...state,
                nodes: nodes(state.nodes, action),
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                fundTreeNodes: fundTree(state.fundTreeNodes, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
            }
            return consolidateState(state, result);

        case types.CHANGE_CONFORMITY_INFO:
            var result = {
                ...state,
                fundTree: fundTree(state.fundTree, action),
                nodes: nodes(state.nodes, action),
                versionValidation: versionValidation(state.versionValidation, action),
                bulkActions: bulkActions(state.bulkActions, action),
                fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action)
            }
            return consolidateState(state, result);
        case types.FUND_FUND_APPROVE_VERSION:

            if (state.closed == false) {
                return {
                    ...state,
                    closed: true,
                }
            }

            return state;

        case types.FUND_FUND_NODES_POLICY_RECEIVE:
        case types.FUND_FUND_NODES_POLICY_REQUEST:
            return {
                ...state,
                fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
            }

        default:
            return state;
    }
}
