import * as types from 'actions/constants/actionTypes';

export function expandFaTreeNode(node) {
    return {
        type: types.FA_EXPAND_FA_TREE,
        node,
    }
}

export function collapseFaTreeNode(node) {
    return {
        type: types.FA_COLLAPSE_FA_TREE,
        node,
    }
}

