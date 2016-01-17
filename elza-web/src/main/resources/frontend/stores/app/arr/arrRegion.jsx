import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'
import faTree from './faTree'

const initialState = {
    activeIndex: null,
    fas: []
}

function selectFaTab(state, action) {
    var faItem = Object.assign({}, action.fa, {faTree: faTree(action.fa.faTree, action), nodes: nodes(undefined, action)});
    var index = indexById(state.fas, action.fa.id);
    if (index == null) {    // není zatím v seznamu, přidáme jí tam
        return {
            ...state,
            fas: [
                ...state.fas,
                faItem
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
        case types.GLOBAL_GET_OBJECT_INFO:
            state.fas.forEach(fa => {
                action.objectInfo.addFa(fa);
                nodes(fa.nodes, action);
            });
            return state
        case types.FA_FA_TREE_REQUEST:
        case types.FA_FA_TREE_RECEIVE:
            var index = indexById(state.fas, action.versionId, "versionId");
            if (index != null) {
                return {
                    ...state,
                    fas: [
                        ...state.fas.slice(0, index),
                        Object.assign({}, state.fas[index], {faTree: faTree(state.fas[index].faTree, action)}),
                        ...state.fas.slice(index + 1)
                    ]
                }
            } else {
                return state;
            }
        case types.FA_FA_TREE_FOCUS_NODE:
        case types.FA_FA_TREE_EXPAND_NODE:
        case types.FA_FA_TREE_COLLAPSE_NODE:
        case types.GLOBAL_CONTEXT_MENU_HIDE:
            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, state.activeIndex),
                    Object.assign({}, state.fas[state.activeIndex], {faTree: faTree(state.fas[state.activeIndex].faTree, action)}),
                    ...state.fas.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_FA_SELECT_SUBNODE:
        case types.FA_FA_CLOSE_NODE_TAB:
        case types.FA_FA_SELECT_NODE_TAB:
            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, state.activeIndex),
                    Object.assign({}, state.fas[state.activeIndex], {nodes: nodes(state.fas[state.activeIndex].nodes, action), faTree: faTree(state.fas[state.activeIndex].faTree, action)}),
                    ...state.fas.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
        case types.FA_SUB_NODE_FORM_VALUE_BLUR:
        case types.FA_SUB_NODE_FORM_VALUE_FOCUS:
        case types.FA_SUB_NODE_FORM_VALUE_ADD:
        case types.FA_SUB_NODE_FORM_VALUE_DELETE:
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
            var faIndex = indexById(state.fas, action.versionId, "versionId");
            if (faIndex != null) {
                return {
                    ...state,
                    fas: [
                        ...state.fas.slice(0, faIndex),
                        Object.assign({}, state.fas[faIndex], {nodes: nodes(state.fas[faIndex].nodes, action), faTree: faTree(state.fas[faIndex].faTree, action)}),
                        ...state.fas.slice(faIndex + 1)
                    ]
                }
            } else {
                return state;
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
        default:
            return state
    }
}

