/**
 * Web api pro komunikaci se serverem.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function fetchRecordIfNeeded(search = '') {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.record.fetched && !state.record.isFetching) {
            return dispatch(fetchRecord(search));
        }
    }
}

export function fetchRecord(search) {
    return dispatch => {
        dispatch(requestRecord())

        return WebApi.findRecord(search)
                .then(json => dispatch(receiveRecord(json)));
    }
}

export function receiveRecord(json) {
    return {
        type: types.RECORD_RECEIVE_RECORD_LIST,
        items: json.recordList,
        countItems: json.count,
        receivedAt: Date.now()
    }
}

export function requestRecord() {
    return {
        type: types.RECORD_REQUEST_RECORD_LIST
    }
}

export function getRecordIfNeeded(recordId) {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.recordData.fetched && !state.recordData.isFetching && recordId !==state.recordData.selectedId) {

            return dispatch(getRecord(recordId));
        }
    }
}

export function getRecord(recordId) {
    return dispatch => {
        dispatch(requestRecordGetRecord())
        return WebApi.getRecord(recordId)
                .then(json => dispatch(receiveRecordGetRecord(recordId, json)));
    }
}

export function requestRecordGetRecord() {
    return {
        type: types.RECORD_REQUEST_RECORD_DETAIL
    }
}

export function receiveRecordGetRecord(recordId, json) {
    return {
        item: json,
        selectedId: recordId,
        type: types.RECORD_RECEIVE_RECORD_DETAIL,
        receivedAt: Date.now()
    }
}



