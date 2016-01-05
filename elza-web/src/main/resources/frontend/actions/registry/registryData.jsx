/**
 * Web api pro komunikaci se serverem.
 */
import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function recordData(record) {
    return {
        type: types.RECORD_SELECT_RECORD,
        record
    }
}

export function recordSearchData(record) {
    return {
        type: types.RECORD_SEARCH_RECORD,
        record
    }
}

export function recordChangeDetail(record) {
    return {
        type: types.RECORD_CHANGE_RECORD_DETAIL,
        record
    }
}
