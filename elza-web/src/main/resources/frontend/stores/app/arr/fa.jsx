import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'
import faTree from './faTree'
import nodes from './nodes'
import bulkActions from './bulkActions'
import versionValidation from './versionValidation'
import {consolidateState} from 'components/Utils'

export function faInitState(faWithVersion) {
    var result = {
        ...faWithVersion,
        id: faWithVersion.versionId,
        closed: faWithVersion.closed,
        faId: faWithVersion.faId,
        versionId: faWithVersion.versionId,
        name: faWithVersion.name,
        isFetching: false,
        dirty: false,
        faTree: faTree(undefined, {type: ''}),
        faTreeMovementsLeft: faTree(undefined, {type: ''}),
        faTreeMovementsRight: faTree(undefined, {type: ''}),
        nodes: nodes(undefined, {type: ''}),
        bulkActions: bulkActions(undefined, {type: ''}),
        versionValidation: versionValidation(undefined, {type: ''})
    }

    result.faTreeMovementsLeft = {...result.faTreeMovementsLeft};
    result.faTreeMovementsLeft.multipleSelection = true;
    result.faTreeMovementsLeft.multipleSelectionOneLevel = true;

    return result;
}

function updateFaTree(state, action) {
    switch (action.area) {
        case types.FA_TREE_AREA_MAIN:
            state.faTree = faTree(state.faTree, action)
            break;
        case types.FA_TREE_AREA_MOVEMENTS_LEFT:
            state.faTreeMovementsLeft = faTree(state.faTreeMovementsLeft, action)
            break;
        case types.FA_TREE_AREA_MOVEMENTS_RIGHT:
            state.faTreeMovementsRight = faTree(state.faTreeMovementsRight, action)
            break;
    }
}

export function fa(state, action) {
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetching: false,
                dirty: true,
                faTree: faTree(state.faTree, action),
                faTreeMovementsLeft: faTree(state.faTreeMovementsLeft, action),
                faTreeMovementsRight: faTree(state.faTreeMovementsRight, action),
                nodes: nodes(state.nodes, action),
                bulkActions: bulkActions(undefined, {type: ''}),
                versionValidation: versionValidation(undefined, {type: ''})
            }
        case types.STORE_SAVE:
            const {id, faId, versionId, name} = state;
            return {
                id,
                faId,
                versionId,
                name,
                faTree: faTree(state.faTree, action),
                faTreeMovementsLeft: faTree(state.faTreeMovementsLeft, action),
                faTreeMovementsRight: faTree(state.faTreeMovementsRight, action),
                nodes: nodes(state.nodes, action),
            }
        case types.FA_FAS_REQUEST:
            if (action.faMap[state.versionId]) {
                return {
                    ...state,
                    isFetching: true,
                }
            } else {
                return state
            }
        case types.FA_FAS_RECEIVE:
            if (action.faMap[state.versionId]) {
                return {
                    ...state,
                    dirty: false,
                    isFetching: false,
                    ...action.faMap[state.versionId]
                }
            } else {
                return state
            }
        case types.CHANGE_FA:
            return {
                ...state,
                dirty: true,
            }
        case types.FA_FA_TREE_REQUEST:
        case types.FA_FA_TREE_RECEIVE:
        case types.FA_FA_TREE_FULLTEXT_RESULT:
        case types.FA_FA_TREE_FULLTEXT_CHANGE:
        case types.FA_FA_TREE_FOCUS_NODE:
        case types.FA_FA_TREE_EXPAND_NODE:
        case types.FA_FA_TREE_COLLAPSE_NODE:
        case types.FA_FA_TREE_COLLAPSE:
        case types.FA_FA_TREE_SELECT_NODE:
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            var result = {...state};
            updateFaTree(result, action);
            return consolidateState(state, result);
        case types.FA_FA_SELECT_SUBNODE:
            var result = {...state, nodes: nodes(state.nodes, action)}
            updateFaTree(result, action);
            return consolidateState(state, result);
        case types.FA_FA_SUBNODES_NEXT:
        case types.FA_FA_SUBNODES_PREV:
        case types.FA_FA_SUBNODES_NEXT_PAGE:
        case types.FA_FA_SUBNODES_PREV_PAGE:
        case types.FA_FA_SUBNODES_FULLTEXT_SEARCH:
        case types.FA_FA_CLOSE_NODE_TAB:
        case types.FA_FA_SELECT_NODE_TAB:
        case types.FA_NODE_CHANGE:
        case types.FA_NODES_RECEIVE:
        case types.FA_NODES_REQUEST:
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
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
        case types.FA_SUB_NODE_FORM_DESC_ITEM_TYPE_DELETE_RESPONSE:
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
        case types.FA_FA_SUBNODES_FULLTEXT_RESULT:
            var result = {...state,
                nodes: nodes(state.nodes, action),
                faTree: faTree(state.faTree, action),
            }
            return consolidateState(state, result);

        case types.CHANGE_DESC_ITEM:
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_MOVE_LEVEL:
            var result = {...state,
                nodes: nodes(state.nodes, action),
                faTree: faTree(state.faTree, action),
                faTreeMovementsLeft: faTree(state.faTreeMovementsLeft, action),
                faTreeMovementsRight: faTree(state.faTreeMovementsRight, action),
            }
            return consolidateState(state, result);

        case types.CHANGE_CONFORMITY_INFO:
            var result = {
                ...state,
                faTree: faTree(state.faTree, action),
                nodes: nodes(state.nodes, action),
                versionValidation: versionValidation(state.versionValidation, action),
                bulkActions: bulkActions(state.bulkActions, action)
            }
            return consolidateState(state, result);
        case types.BULK_ACTIONS_DATA_LOADING:
        case types.BULK_ACTIONS_DATA_LOADED:
        case types.BULK_ACTIONS_RECEIVED_DATA:
        case types.BULK_ACTIONS_VERSION_VALIDATE_RECEIVED_DATA:
        case types.BULK_ACTIONS_RECEIVED_ACTIONS:
        case types.BULK_ACTIONS_RECEIVED_STATES:
        case types.BULK_ACTIONS_RECEIVED_STATE:
        case types.BULK_ACTIONS_STATE_CHANGE:
        case types.BULK_ACTIONS_STATE_IS_DIRTY:
            var result = {...state, bulkActions: bulkActions(state.bulkActions, action)}
            return consolidateState(state, result);
        case types.FA_VERSION_VALIDATION_LOAD:
        case types.FA_VERSION_VALIDATION_RECEIVED:
            var result = {...state, versionValidation: versionValidation(state.versionValidation, action)};
            return consolidateState(state, result);

        case types.FA_FA_APPROVE_VERSION:

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
