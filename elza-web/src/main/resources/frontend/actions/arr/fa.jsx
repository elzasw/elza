import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function selectFaTab(fa, moveToBegin=false) {
    return {
        type: types.FA_SELECT_FA_TAB,
        fa,
        moveToBegin
    }
}

export function closeFaTab(fa) {
    return {
        type: types.FA_CLOSE_FA_TAB,
        fa
    }
}

