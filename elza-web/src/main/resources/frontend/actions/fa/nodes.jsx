import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function selectNode(node, moveToBegin=false) {
    return {
        type: types.FA_SELECT_NODE,
        node,
        moveToBegin
    }
}

export function closeNode(node) {
    return {
        type: types.FA_CLOSE_NODE,
        node
    }
}

