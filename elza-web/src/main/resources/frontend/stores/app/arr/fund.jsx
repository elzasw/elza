import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'
import fundTree from './fundTree'
import nodes from './nodes'
import fundDataGrid from './fundDataGrid'
import bulkActions from './bulkActions'
import versionValidation from './versionValidation'
import {consolidateState} from 'components/Utils'
import {isBulkAction} from 'actions/arr/bulkActions'
import {isFundTreeAction} from 'actions/arr/fundTree'
import {isSubNodeFormAction, isSubNodeFormCacheAction} from 'actions/arr/subNodeForm'
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo'
import {isNodeInfoAction} from 'actions/arr/nodeInfo'
import {isVersionValidation} from 'actions/arr/versionValidation'
import {isNodeAction} from 'actions/arr/node'
import {isNodesAction} from 'actions/arr/nodes'
import {isSubNodeRegisterAction} from 'actions/arr/subNodeRegister'
import {isDeveloperScenariosAction} from 'actions/global/developer'
import {isFundDataGridAction} from 'actions/arr/fundDataGrid'
import {isFundChangeAction} from 'actions/global/change'

export function fundInitState(fundWithVersion) {
    var result = {
        ...fundWithVersion,
        id: fundWithVersion.versionId,
        closed: fundWithVersion.closed,
        fundId: fundWithVersion.fundId,
        versionId: fundWithVersion.versionId,
        name: fundWithVersion.name,
        isFetching: false,
        dirty: false,
        fundDataGrid: fundDataGrid(),
        fundTree: fundTree(undefined, {type: ''}),
        fundTreeMovementsLeft: fundTree(undefined, {type: ''}),
        fundTreeMovementsRight: fundTree(undefined, {type: ''}),
        nodes: nodes(undefined, {type: ''}),
        bulkActions: bulkActions(undefined, {type: ''}),
        versionValidation: versionValidation(undefined, {type: ''})
    }

    result.fundTreeMovementsLeft = {...result.fundTreeMovementsLeft};
    result.fundTreeMovementsLeft.multipleSelection = true;
    result.fundTreeMovementsLeft.multipleSelectionOneLevel = true;

    return result;
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
    }
}

export function fund(state, action) {
    if (isBulkAction(action)) {
        var result = {...state, bulkActions: bulkActions(state.bulkActions, action)}
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
        return consolidateState(state, result);
    }

    if (false
        || isSubNodeFormAction(action)
        || isSubNodeFormCacheAction(action)
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
        || isNodeAction(action)
        || isNodesAction(action)
        || isSubNodeRegisterAction(action)
        || isDeveloperScenariosAction(action)
        || isFundChangeAction(action)
    ) {
        var result = {...state,
            nodes: nodes(state.nodes, action),
            fundTree: fundTree(state.fundTree, action),
        }
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetching: false,
                closed: true,   // při načtení vždy chceme closed, u i když není - aby nemohl editovat, než se načte aktuální stav ze serveru
                dirty: true,
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                nodes: nodes(state.nodes, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
                bulkActions: bulkActions(undefined, {type: ''}),
                versionValidation: versionValidation(undefined, {type: ''})
            }
        case types.STORE_SAVE:
            const {id, fundId, versionId, name, lockDate} = state;
            return {
                id,
                fundId,
                versionId,
                name,
                lockDate,
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                nodes: nodes(state.nodes, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
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
            }
            return consolidateState(state, result);

        case types.CHANGE_CONFORMITY_INFO:
            var result = {
                ...state,
                fundTree: fundTree(state.fundTree, action),
                nodes: nodes(state.nodes, action),
                versionValidation: versionValidation(state.versionValidation, action),
                bulkActions: bulkActions(state.bulkActions, action)
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

        default:
            return state;
    }
}
