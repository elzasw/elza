import * as types from 'actions/constants/actionTypes';

export function faTreeNodeExpand(node) {
    return {
        type: types.FA_FA_TREE_EXPAND_NODE,
        node,
    }
}

export function faTreeNodeCollapse(node) {
    return {
        type: types.FA_FA_TREE_COLLAPSE_NODE,
        node,
    }
}

