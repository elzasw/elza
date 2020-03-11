import * as types from 'actions/constants/ActionTypes.js';
import {getRoutingKeyType} from 'stores/app/utils.jsx';
import fundTree from './fundTree.jsx';
import nodes from './nodes.jsx';
import fundDataGrid from './fundDataGrid.jsx';
import bulkActions from './bulkActions.jsx';
import versionValidation from './versionValidation.jsx';
import fundNodesPolicy from './fundNodesPolicy.jsx';
import fundFiles from './fundFiles.jsx';
import fundOutput from './fundOutput.jsx';
import fundAction from './fundAction.jsx';
import {consolidateState} from 'components/Utils.jsx';
import {isBulkAction} from 'actions/arr/bulkActions.jsx';
import {isFundTreeAction} from 'actions/arr/fundTree.jsx';
import {nodeFormActions, outputFormActions, structureFormActions} from 'actions/arr/subNodeForm.jsx';
import {isSubNodeInfoAction} from 'actions/arr/subNodeInfo.jsx';
import {isNodeInfoAction} from 'actions/arr/nodeInfo.jsx';
import {isVersionValidation} from 'actions/arr/versionValidation.jsx';
import {isNodeAction} from 'actions/arr/node.jsx';
import {isNodesAction} from 'actions/arr/nodes.jsx';
import {isSubNodeDaosAction} from 'actions/arr/subNodeDaos.jsx';
import {isDeveloperScenariosAction} from 'actions/global/developer.jsx';
import {isFundDataGridAction} from 'actions/arr/fundDataGrid.jsx';
import {isFundChangeAction} from 'actions/global/change.jsx';
import {isFundFilesAction} from 'actions/arr/fundFiles.jsx';
import {isFundActionAction} from 'actions/arr/fundAction.jsx';
import {isFundOutput} from 'actions/arr/fundOutput.jsx';
import DetailReducer from 'shared/detail/DetailReducer';
import SimpleListReducer from 'shared/list/simple/SimpleListReducer';
import processAreaStores from 'shared/utils/processAreaStores';
import isCommonArea from 'stores/utils/isCommonArea';
import structureNodeForm from './structureNodeForm';
import {isStructureNodeForm} from '../../../actions/arr/structureNodeForm';

export function fundInitState(fundWithVersion) {
    const result = {
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
        fundFiles: fundFiles(),
        fundTree: fundTree(undefined, { type: '' }),
        fundTreeMovementsLeft: fundTree(undefined, { type: '' }),
        fundTreeMovementsRight: fundTree(undefined, { type: '' }),
        moving: false,
        fundTreeDaosLeft: fundTree(),
        fundTreeDaosRight: fundTree(),
        fundTreeNodes: fundTree(undefined, { type: '' }),
        nodes: nodes(undefined, { type: '' }),
        fundNodesPolicy: fundNodesPolicy(),
        bulkActions: bulkActions(undefined, { type: '' }),
        versionValidation: versionValidation(undefined, { type: '' }),
        fundNodesError: {}, // zatím jen pomocný, je řešeno ve state
        requestList: SimpleListReducer(),   // seznam požadavků na digitalizaci
        requestDetail: DetailReducer(), // detail vybraného požadavku na digitalizaci
        daoPackageList: SimpleListReducer(),   // seznam všech balíčků pro daný AS
        daoUnassignedPackageList: SimpleListReducer(),   // seznam nepřiřazených balíčků pro daný AS
        daoPackageDetail: DetailReducer(), // detail vybraného balíčků
        nodeDaoList: SimpleListReducer(), // seznam DAO pro node
        nodeDaoListAssign: SimpleListReducer(), // seznam DAO pro node sloužící pro node, které jsou sekundární, např. pro přiřazení atp.
        packageDaoList: SimpleListReducer(), // seznam DAO pro balíček
        structureNodeForm: structureNodeForm(),
        reducer: fund,
    };

    result.fundTreeMovementsLeft = { ...result.fundTreeMovementsLeft };
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
            state.fundTree = fundTree(state.fundTree, action);
            break;
        case types.FUND_TREE_AREA_MOVEMENTS_LEFT:
            state.fundTreeMovementsLeft = fundTree(state.fundTreeMovementsLeft, action);
            break;
        case types.FUND_TREE_AREA_MOVEMENTS_RIGHT:
            state.fundTreeMovementsRight = fundTree(state.fundTreeMovementsRight, action);
            break;
        case types.FUND_TREE_AREA_NODES:
            state.fundTreeNodes = fundTree(state.fundTreeNodes, action);
            break;
        case types.FUND_TREE_AREA_DAOS_LEFT:
            state.fundTreeDaosLeft = fundTree(state.fundTreeDaosLeft, action);
            break;
        case types.FUND_TREE_AREA_DAOS_RIGHT:
            state.fundTreeDaosRight = fundTree(state.fundTreeDaosRight, action);
            break;
        default:
            break;
    }
}

export function fund(state, action) {
    if (isCommonArea(action.area)) {
        return processAreaStores(state, action);
    }

    if (isBulkAction(action)) {
        const result = { ...state, bulkActions: bulkActions(state.bulkActions, action) };
        return consolidateState(state, result);
    }
    if (isFundActionAction(action)) {
        const result = { ...state, fundAction: fundAction(state.fundAction, action) };
        return consolidateState(state, result);
    }

    if (isFundOutput(action)) {
        const result = { ...state, fundOutput: fundOutput(state.fundOutput, action) };
        return consolidateState(state, result);
    }

    if (isStructureNodeForm(action)) {
        const result = { ...state, structureNodeForm: structureNodeForm(state.structureNodeForm, action) };
        return consolidateState(state, result);
    }

    if (isFundFilesAction(action)) {
        const result = { ...state, fundFiles: fundFiles(state.fundFiles, action) };
        return consolidateState(state, result);
    }

    if (isFundTreeAction(action)) {
        const result = { ...state };
        updateFundTree(result, action);
        return consolidateState(state, result);
    }

    if (isVersionValidation(action)) {
        const result = { ...state, versionValidation: versionValidation(state.versionValidation, action) };
        return consolidateState(state, result);
    }

    if (isFundDataGridAction(action)) {
        const result = { ...state, fundDataGrid: fundDataGrid(state.fundDataGrid, action) };
        return consolidateState(state, result);
    }

    if (nodeFormActions.isSubNodeFormAction(action)) {
        const type = getRoutingKeyType(action.routingKey);
        switch (type) {
            case 'NODE': {
                const result = {
                    ...state,
                    nodes: nodes(state.nodes, action),
                    fundTree: fundTree(state.fundTree, action),
                };
                return consolidateState(state, result);
            }
            case 'DATA_GRID': {
                const result = { ...state, fundDataGrid: fundDataGrid(state.fundDataGrid, action) };
                return consolidateState(state, result);
            }
            default:
                break;
        }
    } else if (outputFormActions.isSubNodeFormAction(action)) {
        const result = { ...state, fundOutput: fundOutput(state.fundOutput, action) };
        return consolidateState(state, result);
    } else if (structureFormActions.isSubNodeFormAction(action)) {
        const result = { ...state, structureNodeForm: structureNodeForm(state.structureNodeForm, action) };
        return consolidateState(state, result);
    }

    if (false
        || nodeFormActions.isSubNodeFormCacheAction(action, 'NODE')
        || isSubNodeInfoAction(action)
        || isNodeInfoAction(action)
        || isNodeAction(action)
        || isNodesAction(action)
        || isSubNodeDaosAction(action)
        || isDeveloperScenariosAction(action)
    ) {
        const result = {
            ...state,
            nodes: nodes(state.nodes, action),
            fundTree: fundTree(state.fundTree, action),
        };
        return consolidateState(state, result);
    }

    if (false || isFundChangeAction(action)) {
        const result = {
            ...state,
            nodes: nodes(state.nodes, action),
            fundTree: fundTree(state.fundTree, action),
            fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
            fundNodesError: {},
        };
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
                moving: false,
                fundTreeDaosLeft: fundTree(state.fundTreeDaosLeft, action),
                fundTreeDaosRight: fundTree(state.fundTreeDaosRight, action),
                fundTreeNodes: initFundTreeNodes(fundTree()),
                nodes: nodes(state.nodes, action),
                fundOutput: fundOutput(state.fundOutput, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
                fundFiles: fundFiles(state.fundFiles, action),
                fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
                bulkActions: bulkActions(undefined, { type: '' }),
                fundAction: fundAction(undefined, { type: '' }),
                versionValidation: versionValidation(undefined, { type: '' }),
                fundNodesError: {},
                requestList: SimpleListReducer(),
                requestDetail: DetailReducer(),
                daoPackageList: SimpleListReducer(),
                daoUnassignedPackageList: SimpleListReducer(),
                daoPackageDetail: DetailReducer(),
                nodeDaoList: SimpleListReducer(),
                nodeDaoListAssign: SimpleListReducer(),
                packageDaoList: SimpleListReducer(),
                structureNodeForm: structureNodeForm(),
                reducer: fund,
            };
        case types.STORE_SAVE:
            const { id, versionId, name, lockDate, activeVersion, lastUseTemplateName } = state;
            return {
                id,
                activeVersion,
                versionId,
                name,
                lockDate,
                lastUseTemplateName,
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                fundTreeDaosLeft: fundTree(state.fundTreeDaosLeft, action),
                fundTreeDaosRight: fundTree(state.fundTreeDaosRight, action),
                nodes: nodes(state.nodes, action),
                fundOutput: fundOutput(state.fundOutput, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
                fundFiles: fundFiles(state.fundFiles, action),
            };
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_CHANGES_DETAIL:
        case types.OUTPUT_INCREASE_VERSION:
        case types.OUTPUT_STATE_CHANGE:
        case types.CHANGE_OUTPUTS: {
            const result = {
                ...state,
                fundOutput: fundOutput(state.fundOutput, action),
            };
            return consolidateState(state, result);
        }
        case types.FUND_FUND_CHANGE_READ_MODE: {
            const result = {
                ...state,
                nodes: nodes(state.nodes, action),
                fundOutput: fundOutput(state.fundOutput, action),
            };
            return consolidateState(state, result);
        }
        case types.FUND_TEMPLATE_USE: {
            return {
                ...state,
                lastUseTemplateName: action.template.name,
            };
        }
        case types.CHANGE_FUND_ACTION: {
            const result = {
                ...state,
                fundAction: fundAction(state.fundAction, action),
                fundOutput: fundOutput(state.fundOutput, action),
            };
            return consolidateState(state, result);
        }
        case types.CHANGE_FILES: {
            return {
                ...state,
                fundFiles: fundFiles(state.fundFiles, action),
            };
        }
        case types.FUND_FUNDS_REQUEST:
            if (action.fundMap[state.versionId]) {
                return {
                    ...state,
                    isFetching: true,
                };
            } else {
                return state;
            }
        case types.FUND_FUNDS_RECEIVE:
            if (action.fundMap[state.versionId]) {
                return {
                    ...state,
                    dirty: false,
                    isFetching: false,
                    ...action.fundMap[state.versionId],
                };
            } else {
                return state;
            }
        case types.CHANGE_FUND:
            return {
                ...state,
                dirty: true,
            };
        case types.GLOBAL_CONTEXT_MENU_HIDE: {
            const result = { ...state };
            updateFundTree(result, action);
            return consolidateState(state, result);
        }
        case types.FUND_FUND_SELECT_SUBNODE: {
            const result = { ...state, nodes: nodes(state.nodes, action) };
            updateFundTree(result, action);
            return consolidateState(state, result);
        }
        case types.FUND_NODE_CHANGE:
        case types.FUND_NODES_RECEIVE:
        case types.FUND_NODES_REQUEST: {
            const result = {
                ...state,
                nodes: nodes(state.nodes, action),
                fundTree: fundTree(state.fundTree, action),
            };
            return consolidateState(state, result);
        }
        case types.FUND_NODE_INCREASE_VERSION:
        case types.CHANGE_FUND_RECORD:
        case types.CHANGE_NODES:
        case types.FUND_SUBNODE_UPDATE:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_MOVE_LEVEL: {
            const result = {
                ...state,
                nodes: nodes(state.nodes, action),
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                fundTreeDaosLeft: fundTree(state.fundTreeDaosLeft, action),
                fundTreeDaosRight: fundTree(state.fundTreeDaosRight, action),
                fundTreeNodes: fundTree(state.fundTreeNodes, action),
                fundDataGrid: fundDataGrid(state.fundDataGrid, action),
            };
            return consolidateState(state, result);
        }
        case types.FUND_NODES_MOVE_START: {
            const result = {
                ...state,
                moving: true,
            };
            return consolidateState(state, result);
        }
        case types.FUND_NODES_MOVE_STOP: {
            const result = {
                ...state,
                moving: false,
            };
            return consolidateState(state, result);
        }
        case types.CHANGE_CONFORMITY_INFO:
        case types.CHANGE_NODE_REQUESTS: {
            const result = {
                ...state,
                fundTree: fundTree(state.fundTree, action),
                nodes: nodes(state.nodes, action),
                versionValidation: versionValidation(state.versionValidation, action),
                bulkActions: bulkActions(state.bulkActions, action),
                fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
                fundNodesError: {}, // nová instance
            };
            return consolidateState(state, result);
        }
        case types.FUND_FUND_APPROVE_VERSION:

            if (state.closed === false) {
                return {
                    ...state,
                    closed: true,
                };
            }

            return state;

        case types.FUND_FUND_NODES_POLICY_RECEIVE:
        case types.FUND_FUND_NODES_POLICY_REQUEST:
            return {
                ...state,
                fundNodesPolicy: fundNodesPolicy(state.fundNodesPolicy, action),
            };

        case types.FUND_INVALID:
            const result = {
                ...state,
                fundAction: fundAction(state.fundAction, action),
                fundOutput: fundOutput(state.fundOutput, action),
                nodes: nodes(state.nodes, action),
            };
            return consolidateState(state, result);

        case types.NODES_DELETE: {
            const result = {
                ...state,
                nodes: nodes(state.nodes, action),
                fundTree: fundTree(state.fundTree, action),
                fundTreeMovementsLeft: fundTree(state.fundTreeMovementsLeft, action),
                fundTreeMovementsRight: fundTree(state.fundTreeMovementsRight, action),
                fundTreeNodes: fundTree(state.fundTreeNodes, action),
            };
            return consolidateState(state, result);
        }

        default:
            return state;
    }
}
