/**
 * Akce pro hromadné akce
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';

export function isFundActionsAction(action) {
    switch (action) {
        case types.FUND_ACTIONS_ACTION_SELECT:
        case types.FUND_ACTIONS_ACTION_DETAIL_REQUEST:
        case types.FUND_ACTIONS_ACTION_DETAIL_RECEIVE:
            return true;
        default:
            return false;
    }
}

export function fundActionsFetchListIfNeeded(dataKey) {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        if (activeIndex) {
            const {fundActions: {detail: {currentDataKey}}} = funds[activeIndex];
            if (currentDataKey !== dataKey) {
                dispatch(fundActionsListRequest(dataKey));
                WebApi.getBulkActionsState(dataKey).then(data => dispatch(fundActionsListReceive(dataKey, data)));
            }
        }
    }
}
export function fundActionsFetchDetailIfNeeded(dataKey) {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        if (activeIndex) {
            const {fundActions: {detail: {currentDataKey}}} = funds[activeIndex];
            if (currentDataKey !== dataKey) {
                dispatch(fundActionsActionRequest(dataKey));
                WebApi.getBulkActionsState(dataKey).then(data => dispatch(fundActionsActionReceive(dataKey, data)));
            }
        }
    }
}

export function fundActionsListRequest(dataKey) {
    return {
        type: types.FUND_ACTIONS_LIST_REQUEST,
        versionId: dataKey,
        dataKey
    }
}

export function fundActionsListReceive(dataKey, data) {
    return {
        type: types.FUND_ACTIONS_LIST_RECEIVE,
        versionId: dataKey,
        dataKey,
        data
    }
}

export function fundActionsActionRequest(dataKey) {
    return {
        type: types.FUND_ACTIONS_,
        versionId: dataKey,
        dataKey
    }
}

export function fundActionsActionReceive(dataKey, data) {
    return {
        type: types.FUND_ACTIONS_,
        versionId: dataKey,
        dataKey,
        data
    }
}
