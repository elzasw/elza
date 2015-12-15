import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function faSelectNodeTab(node, moveToBegin=false) {
    return {
        type: types.FA_FA_SELECT_NODE_TAB,
        node,
        moveToBegin
    }
}

export function faCloseNodeTab(node) {
    return {
        type: types.FA_FA_CLOSE_NODE_TAB,
        node
    }
}

