/**
 * Akce pro archivní soubory.
 */

import * as types from 'actions/constants/ActionTypes';
import { WebApi } from 'actions/index.jsx';
import { Api } from "../../api";

import { DEFAULT_LIST_SIZE } from '../../constants.tsx';
import { downloadExportFile } from '../global/downloadExportFile.ts';

export const DEFAULT_FUND_LIST_MAX_SIZE = DEFAULT_LIST_SIZE;

function _fundRegionDataKey(fundRegion) {
    return fundRegion.filterText + '_';
}

function _fundDetailDataKey(fundDetail) {
    if (fundDetail.id !== null) {
        return fundDetail.id + '_';
    } else {
        return '';
    }
}

/**
 * Vybere daný fond pro zobrazení. Pokud již daný fond byl otevřen, použije nastavení jeho posledního zobrazení.
 */
export function fundsSelectFund(id) {
    return {
        type: types.FUNDS_SELECT_FUND,
        id,
    };
}

export function fundsSearch(filterText) {
    return {
        type: types.FUNDS_SEARCH,
        filterText,
    };
}

/**
 * Fetch dat pro detail archivního souboru.
 */
export function fundsFundDetailFetchIfNeeded() {
    return (dispatch, getState) => {
        const state = getState();
        const fundDetail = state.fundRegion.fundDetail;
        const dataKey = _fundDetailDataKey(fundDetail);

        if (fundDetail.currentDataKey !== dataKey) {
            dispatch(fundsFundDetailRequest(dataKey));
            WebApi.getFundDetail(fundDetail.id).then(json => {
                const newState = getState();
                const newFundDetail = newState.fundRegion.fundDetail;
                const newDataKey = _fundDetailDataKey(newFundDetail);
                if (newDataKey === dataKey) {
                    dispatch(fundsFundDetailReceive(json));
                }
            });
        }
    };
}

/**
 * Fetch dat pro detail archivního souboru.
 */
export async function getFundDetail(id) {
    const fundDetail = await WebApi.getFundDetail(id);
    return fundDetail;
}

/**
 * Fetch dat pro seznam archivních souborů.
 */
export function fundsFetchIfNeeded(size = DEFAULT_FUND_LIST_MAX_SIZE) {
    return async (dispatch, getState) => {
        const state = getState();
        const { fundRegion } = state;
        const { filter } = fundRegion;
        const dataKey = _fundRegionDataKey(fundRegion);

        if (fundRegion.currentDataKey !== dataKey) {
            dispatch(fundsRequest(dataKey));

            const { data } = await Api.funds.fundSearchFunds({
                filters: filter.filter?.map((filter) => filter.getFilterValue(filter)),
                size,
                offset: filter.from
            });

            const newState = getState();
            const newFundRegion = newState.fundRegion;
            const newDataKey = _fundRegionDataKey(newFundRegion);

            if (newDataKey === dataKey) {
                dispatch(fundsReceive(data));
            }
        }
    };
}

export function fundsExportResults() {
    return async (dispatch, getState) => {
        const { fundRegion } = getState();
        const { filter } = fundRegion;

        const { data: fileId } = await Api.funds.fundExportFunds({
            filters: filter.filter?.map((filter) => filter.getFilterValue(filter))
        });

        dispatch(downloadExportFile(fileId));
    }
}

function fundsRequest(dataKey) {
    return {
        type: types.FUNDS_REQUEST,
        dataKey,
    };
}

function fundsReceive(data) {
    return {
        type: types.FUNDS_RECEIVE,
        data,
    };
}

function fundsFundDetailRequest(dataKey) {
    return {
        type: types.FUNDS_FUND_DETAIL_REQUEST,
        dataKey,
    };
}

function fundsFundDetailReceive(data) {
    return {
        type: types.FUNDS_FUND_DETAIL_RECEIVE,
        data,
    };
}

/**
 * Filtr archivních souborů
 *
 * @param filter {Object} - objekt filtru
 */
export function fundsFilter(filter) {
    return {
        type: types.FUNDS_FILTER,
        filter,
    };
}
