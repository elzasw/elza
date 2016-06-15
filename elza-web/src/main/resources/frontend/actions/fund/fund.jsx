/**
 * Akce pro archivní soubory.
 */

import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';

function _fundRegionDataKey(fundRegion) {
    return fundRegion.filterText + '_'
}

function _fundDetailDataKey(fundDetail) {
    if (fundDetail.id !== null) {
        return fundDetail.id + '_'
    } else {
        return ''
    }
}

export function fundsSelectFund(id) {
    return {
        type: types.FUNDS_SELECT_FUND,
        id,
    }
}

export function fundsSearch(filterText) {
    return {
        type: types.FUNDS_SEARCH,
        filterText,
    }
}

/**
 * Fetch dat pro detail archivního souboru.
 */
export function fundsFundDetailFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const fundDetail = state.fundRegion.fundDetail
        const dataKey = _fundDetailDataKey(fundDetail)

        if (fundDetail.currentDataKey !== dataKey) {
            dispatch(fundsFundDetailRequest(dataKey))
            WebApi.getFundDetail(fundDetail.id)
                .then(json => {
                    var newState = getState();
                    const newFundDetail = newState.fundRegion.fundDetail;
                    const newDataKey = _fundDetailDataKey(newFundDetail)
                    if (newDataKey === dataKey) {
                        dispatch(fundsFundDetailReceive(json))
                    }
                })
        }
    }
}

/**
 * Fetch dat pro seznam archivních souborů.
 */
export function fundsFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        const fundRegion = state.fundRegion;
        const dataKey = _fundRegionDataKey(fundRegion)

        if (fundRegion.currentDataKey !== dataKey) {
            dispatch(fundsRequest(dataKey))
            WebApi.findFunds(fundRegion.filterText)
                .then(json => {
                    var newState = getState();
                    const newFundRegion = newState.fundRegion;
                    const newDataKey = _fundRegionDataKey(newFundRegion)
                    if (newDataKey === dataKey) {
                        dispatch(fundsReceive(json))
                    }
                })
        }
    }
}

function fundsRequest(dataKey) {
    return {
        type: types.FUNDS_REQUEST,
        dataKey,
    }
}

function fundsReceive(data) {
    return {
        type: types.FUNDS_RECEIVE,
        data,
    }
}

function fundsFundDetailRequest(dataKey) {
    return {
        type: types.FUNDS_FUND_DETAIL_REQUEST,
        dataKey,
    }
}

function fundsFundDetailReceive(data) {
    return {
        type: types.FUNDS_FUND_DETAIL_RECEIVE,
        data,
    }
}