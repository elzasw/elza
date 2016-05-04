/**
 * Akce pro hromadné akce
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';

export function isFundActionAction(action) {
    switch (action.type) {
        case types.FUND_ACTION_ACTION_SELECT:
        case types.FUND_ACTION_CONFIG_REQUEST:
        case types.FUND_ACTION_CONFIG_RECEIVE:
        case types.FUND_ACTION_ACTION_DETAIL_REQUEST:
        case types.FUND_ACTION_ACTION_DETAIL_RECEIVE:
        case types.FUND_ACTION_LIST_REQUEST:
        case types.FUND_ACTION_LIST_RECEIVE:
        case types.FUND_ACTION_FORM_SHOW:
        case types.FUND_ACTION_FORM_HIDE:
        case types.FUND_ACTION_FORM_RESET:
        case types.FUND_ACTION_FORM_CHANGE:
        case types.FUND_ACTION_FORM_SUBMIT:
            return true;
        default:
            return false;
    }
}

export function fundActionFetchListIfNeeded(dataKey) {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        if (activeIndex !== null && funds[activeIndex].fundAction) {
            const {fundAction: {list: {currentDataKey}}, versionId} = funds[activeIndex];
            if (currentDataKey !== dataKey) {
                dispatch(fundActionListRequest(dataKey));
                WebApi.getBulkActionsList(dataKey).then(data => dispatch(fundActionListReceive(dataKey, data, versionId)));
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}
export function fundActionFetchConfigIfNeeded(dataKey) {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        if (activeIndex !== null && funds[activeIndex].fundAction) {
            const {fundAction: {config: {currentDataKey}}, versionId} = funds[activeIndex];
            if (currentDataKey !== dataKey) {
                dispatch(fundActionConfigRequest(dataKey));
                WebApi.getBulkActions(dataKey).then(data => dispatch(fundActionConfigReceive(dataKey, data, versionId)));
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}

function _dispatchInActualFund(action) {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        const fund = funds[activeIndex];
        if (fund) {
            dispatch({...action, versionId: fund.versionId});
        } else {
            window.console.error('Active fund not found');
        }
    }
}

export function fundActionFetchDetailIfNeeded() {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        if (activeIndex !== null) {
            const {fundAction : {detail: {currentDataKey, data, isFetching, fetched} }, versionId} = funds[activeIndex];
            const isNotSameDataKey = data && currentDataKey !== data.id;
            if ((isNotSameDataKey && !isFetching) || (!isFetching && !fetched && currentDataKey !== null)) {
                dispatch(fundActionActionRequest(currentDataKey, versionId));
                WebApi.getBulkAction(currentDataKey).then(data => dispatch(fundActionActionReceive(currentDataKey, data, versionId)));
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}

export function fundActionActionSelect(dataKey) {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_ACTION_SELECT,
        dataKey
    });
}

export function fundActionListRequest(dataKey) {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_LIST_REQUEST,
        dataKey
    });
}

export function fundActionListReceive(dataKey, data, versionId) {
    return {
        type: types.FUND_ACTION_LIST_RECEIVE,
        versionId,
        dataKey,
        data
    }
}

export function fundActionConfigRequest(dataKey) {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_CONFIG_REQUEST,
        dataKey
    });
}

export function fundActionConfigReceive(dataKey, data, versionId) {
    return {
        type: types.FUND_ACTION_CONFIG_RECEIVE,
        versionId,
        dataKey,
        data
    }
}

export function fundActionActionRequest(dataKey) {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_ACTION_DETAIL_REQUEST,
        dataKey
    });
}

export function fundActionActionReceive(dataKey, data, versionId) {
    return {
        type: types.FUND_ACTION_ACTION_DETAIL_RECEIVE,
        versionId,
        dataKey,
        data
    }
}

export function fundActionFormShow() {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_FORM_SHOW
    });
}

export function fundActionFormHide() {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_FORM_HIDE
    });
}

export function fundActionFormReset() {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_FORM_RESET
    });
}

export function fundActionFormChange(data) {
    return _dispatchInActualFund({
        type: types.FUND_ACTION_FORM_CHANGE,
        data
    });
}

export function fundActionFormSubmit() {
    return (dispatch, getState) => {
        const {arrRegion: {funds, activeIndex}} = getState();
        if (activeIndex !== null) {
            const {fundAction : {form, isFormVisible}, versionId} = funds[activeIndex];
            if (isFormVisible) {
                const nodeIds = form.nodeList.map(node => node.id);
                WebApi.queueBulkActionWithIds(versionId, form.code, nodeIds);
                dispatch({
                    type: types.FUND_ACTION_FORM_SUBMIT
                })
            }
        } else {
            window.console.error('Active fund not found');
        }
    }
}
