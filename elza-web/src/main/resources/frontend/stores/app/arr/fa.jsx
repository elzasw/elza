import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'
import faTree from './faTree'
import nodes from './nodes'

export function faInitState(faWithVersion) {
    var result = {
        ...faWithVersion,
        isFetching: false,
        fetched: false,
        dirty: false,
        faTree: faTree(undefined, {type: ''}),
        faTreeMovementsLeft: faTree(undefined, {type: ''}),
        faTreeMovementsRight: faTree(undefined, {type: ''}),
        nodes: nodes(undefined, {type: ''})
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

const faInitialState = {
}

export function fa(state = nodeInitialState, action) {
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetching: false,
                fetched: false,
                dirty: false,
                faTree: faTree(state.faTree, action),
                faTreeMovementsLeft: faTree(state.faTreeMovementsLeft, action),
                faTreeMovementsRight: faTree(state.faTreeMovementsRight, action),
                nodes: nodes(state.nodes, action),
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
            return result;
        case types.FA_FA_SELECT_SUBNODE:
            var result = {...state, nodes: nodes(state.nodes, action)}
            updateFaTree(result, action);
            return result;
        case types.FA_FA_SUBNODES_NEXT:
        case types.FA_FA_SUBNODES_PREV:
        case types.FA_FA_SUBNODES_NEXT_PAGE:
        case types.FA_FA_SUBNODES_PREV_PAGE:
        case types.FA_FA_CLOSE_NODE_TAB:
        case types.FA_FA_SELECT_NODE_TAB:
        case types.FA_NODE_CHANGE:
            return {...state, nodes: nodes(state.nodes, action)}
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
            return {...state, nodes: nodes(state.nodes, action)}
        case types.CHANGE_CONFORMITY_INFO:
            return {...state, faTree: faTree(state.faTree, action), nodes: nodes(state.nodes, action)}
        default:
            return state;
    }
}