/**
 * Akce pro vyhledávání archivních souborů v komponentě Modal
 */
import * as types from './../../actions/constants/ActionTypes';
import { Api } from 'api';
import { FilterType, FieldType } from 'elza-api';
import { isMaskViewDefinition, matchesMask, unmaskString } from '../../components/arr/node-edit/desc-items/maskUtils';

export const getDataKey = (fulltext, isIdSearch) => {
    return isIdSearch ? `${fulltext}-id` : fulltext;
}

export const generateDataKey = (filters) => {
    return JSON.stringify(filters.map((filter) => filter.getSerializedString(filter)));
}

export function isFundSearchAction(action) {
    switch (action.type) {
        case types.FUND_SEARCH_FULLTEXT_CHANGE:
        case types.FUND_SEARCH_FULLTEXT_REQUEST:
        case types.FUND_SEARCH_FULLTEXT_RECEIVE:
        case types.FUND_SEARCH_EXPAND_FUND:
        case types.FUND_SEARCH_FUND_REQUEST:
        case types.FUND_SEARCH_FUND_RECEIVE:
            return true;
        default:
            return false;
    }
}

function unmaskSingleFieldValue(filterValue, itemType) {
    if (
        filterValue.filterType === FilterType.FieldValue &&
        filterValue.field?.fieldType === FieldType.DescItem &&
        filterValue.value &&
        itemType
    ) {
        const viewDefinition = itemType.viewDefinition;
        if (isMaskViewDefinition(viewDefinition) && matchesMask(filterValue.value, viewDefinition.mask, true)) {
            return { ...filterValue, value: unmaskString(filterValue.value, viewDefinition.mask) };
        }
    }
    return filterValue;
}

function unmaskFilterValue(filter) {
    const filterValue = filter.getFilterValue(filter);
    const itemType = filter.data?.itemType;

    if (filterValue.filterType === FilterType.Logical && Array.isArray(filterValue.filters)) {
        return {
            ...filterValue,
            filters: filterValue.filters.map((inner) => unmaskSingleFieldValue(inner, itemType)),
        };
    }
    return unmaskSingleFieldValue(filterValue, itemType);
}

export function fundSearchFetchIfNeeded(force = false) {
    return async (dispatch, getState) => {
        const {
            arrRegion: { fundSearch },
        } = getState();
        const { currentDataKey, filters, funds } = fundSearch;
        if (!filters || filters.length <= 0) { return; }
        const newDataKey = generateDataKey(filters);

        if (newDataKey !== currentDataKey || force) {
            dispatch(fundSearchFulltextRequest());
            const { data } = await Api.node.nodeSearch({
                filters: filters?.map((filter) => {
                    const result = unmaskFilterValue(filter);
                    return result;
                }),
                // size,
                // offset: filters.from
            })
            dispatch(fundSearchFulltextReceive({
                funds: data.fonds,
                partialResult: data.partialResult,
                totalCount: data.totalCount
            }));
        }

        funds.forEach(async (fund) => {
            if (fund.expanded && !fund.isFetching && !fund.fetched) {
                dispatch(fundSearchFundRequest(fund));

                const { data } = await Api.node.nodeGetSearchResult(fund.id)
                dispatch(fundSearchFundReceive(fund, data));
            }
        });
    };
}

export function fundSearchFulltextChange({ fulltext, isIdSearch }) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        fulltext,
        isIdSearch,
    };
}

export function fundSearchFiltersChange({ filters }) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        filters,
    };
}

export function fundSearchFiltersClear() {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        filters: [],
    };
}

export function fundSearchFulltextClear() {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        fulltext: '',
    };
}

function fundSearchFulltextRequest() {
    return {
        type: types.FUND_SEARCH_FULLTEXT_REQUEST,
    };
}

function fundSearchFulltextReceive({ funds, partialResult, totalCount }) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_RECEIVE,
        funds,
        partialResult,
        totalCount,
    };
}

function fundSearchFundRequest(fund) {
    return {
        type: types.FUND_SEARCH_FUND_REQUEST,
        fund,
    };
}

function fundSearchFundReceive(fund, nodes) {
    return {
        type: types.FUND_SEARCH_FUND_RECEIVE,
        fund,
        nodes,
    };
}

export function fundSearchExpandFund(fund) {
    return {
        type: types.FUND_SEARCH_EXPAND_FUND,
        fund,
    };
}
