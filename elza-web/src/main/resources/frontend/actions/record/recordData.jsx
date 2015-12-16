import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function recordData(record, moveToBegin=false) {
    return {
        type: types.RECORD_SELECT_RECORD,
        record,
        moveToBegin
    }
}