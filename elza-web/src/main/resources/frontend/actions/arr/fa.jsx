import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function selectFa(fa, moveToBegin=false) {
    return {
        type: types.FA_SELECT_FA,
        fa,
        moveToBegin
    }
}

export function closeFa(fa) {
    return {
        type: types.FA_CLOSE_FA,
        fa
    }
}

