/**
 * Web api pro komunikaci se serverem.
 */
import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function recordData(record, moveToBegin=false) {
    return {
        type: types.RECORD_SELECT_RECORD,
        record,
        moveToBegin
    }
}

export function recordSearchData(record, moveToBegin=false) {
    return {
        type: types.RECORD_SEARCH_RECORD,
        record,
        moveToBegin
    }
}

export function recordChangeDetail(record, moveToBegin=false) {
    return {
        type: types.RECORD_CHANGE_RECORD_DETAIL,
        record,
        moveToBegin
    }
}
