import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function faSelectNode(node, moveToBegin=false) {
    return {
        type: types.FA_FA_SELECT_NODE,
        node,
        moveToBegin
    }
}

export function faCloseNode(node) {
    return {
        type: types.FA_FA_CLOSE_NODE,
        node
    }
}

