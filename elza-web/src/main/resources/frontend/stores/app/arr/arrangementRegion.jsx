import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'
import faTree from './faTree'
import faTreeData from './faTreeData'
import nodeForm from './nodeForm'

const initialState = {
    activeIndex: null,
    faTreeData: faTreeData(undefined, {}),
    nodeForm: nodeForm(undefined, {}),
    fas: []
}

function selectFaTab(state, action) {
    var faItem = Object.assign({}, action.fa, {faTree: faTree(action.fa.faTree, action), nodes: nodes(undefined, action)});
    var index = indexById(state.fas, action.fa.id);
    if (index == null) {    // není zatím v seznamu, přidáme jí tam
        if (action.moveTabToBegin) {
            return {
                ...state,
                fas: [
                    faItem,
                    ...state.fas
                ],
                activeIndex: 0
            }
        } else {
            return {
                ...state,
                fas: [
                    ...state.fas,
                    faItem
                ],
                activeIndex: state.fas.length
            }
        }
    } else {
        if (action.moveTabToBegin) {
            return {
                ...state,
                fas: [
                    state.fas[index],
                    ...state.fas.slice(0, index),
                    ...state.fas.slice(index + 1)
                ],
                activeIndex: 0
            }
        } else {
            return {
                ...state,
                activeIndex: index
            }
        }
    }
}

export default function arrangementRegion(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_GET_OBJECT_INFO:
            state.fas.forEach(fa => {
                action.objectInfo.addFa(fa);
                nodes(fa.nodes, action);
            });
            return state
        case types.FA_NODE_FORM_REQUEST:
        case types.FA_NODE_FORM_RECEIVE:
            return Object.assign({}, state, {nodeForm: nodeForm(state.nodeForm, action)});
        case types.FA_FA_TREE_EXPAND_NODE:
        case types.FA_FA_TREE_COLLAPSE_NODE:
            return {
                ...state,
                fas: [
                    ...state.fas.slice(0, state.activeIndex),
                    Object.assign({}, state.fas[state.activeIndex], {faTree: faTree(state.fas[state.activeIndex].faTree, action)}),
                    ...state.fas.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_FA_TREE_REQUEST:
            return {
                ...state,
                faTreeData: faTreeData(state.faTreeData, action)
            }
        case types.FA_FA_TREE_RECEIVE:
            return {
                ...state,
                faTreeData: faTreeData(state.faTreeData, action)
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

