import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function fetchRecordIfNeeded() {

    return (dispatch, getState) => {
        var state = getState();

        if (!state.fetched && !state.isFetching) {
            return dispatch(fetchRecord());
        }
    }
}

export function fetchRecord() {
    return dispatch => {

        dispatch(requestRecord())

        return WebApi.getRecord()
                .then(json => dispatch(receiveRecord(json)));
    }
}

export function receiveRecord(json) {
    return {
        type: types.RECORD_RECEIVE_RECORD_LIST,
        items: json,
        receivedAt: Date.now()
    }
}

export function requestRecord() {

    return {
        type: types.RECORD_REQUEST_RECORD_LIST
    }
}

export function fetchRecordDetailIfNeeded() {

    return (dispatch, getState) => {
        var state = getState();

        if (!state.fetched && !state.isFetching) {
            return dispatch(fetchRecordDetail());
        }
    }
}

export function fetchRecordDetail() {
    return dispatch => {

        dispatch(requestRecordDetail())

        return WebApi.findRecord()
                .then(json => dispatch(receiveRecord(json)));
    }
}

export function requestRecordDetail() {

    return {
        type: types.RECORD_REQUEST_RECORD_DETAIL
    }
}

export function receiveRecordDetail() {

    return {
        type: types.RECORD_RECEIVE_RECORD_DETAIL
    }
}



