/**
 * Akce pro vyhledávání archivních souborů v komponentě Modal
 */
import {WebApi} from './../../actions/index.jsx';
import * as types from './../../actions/constants/ActionTypes.js';

export function isFundSearchAction(action) {
    switch (action.type) {
        case types.FUND_SEARCH_FULLTEXT_CHANGE:
        case types.FUND_SEARCH_FULLTEXT_REQUEST:
        case types.FUND_SEARCH_FULLTEXT_RECEIVE:
        case types.FUND_SEARCH_EXPAND_FUND:
        case types.FUND_SEARCH_COLLAPSE_FUND:
        case types.FUND_SEARCH_FUND_REQUEST:
        case types.FUND_SEARCH_FUND_RECEIVE:
            return true;
        default:
            return false
    }
}

export function fundSearchFetchIfNeeded() {
    return (dispatch, getState) => {
        const {arrRegion: {fundSearch}} = getState();
        const {currentDataKey, isFetching, fulltext} = fundSearch;

        if ((fulltext != currentDataKey && !isFetching)) {
            dispatch(fundSearchFulltextRequest(fulltext));
            WebApi.fundFulltext(fulltext).then(result => {
                dispatch(fundSearchFulltextReceive(result));
            });
        }

    }
}

export function fundSearchFulltextChange(fulltext) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        fulltext
    }
}

export function fundSearchFulltextClear() {
    return {
        type: types.FUND_SEARCH_FULLTEXT_CHANGE,
        fulltext: ''
    }
}

function fundSearchFulltextRequest(fulltext) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_REQUEST,
        fulltext
    }
}

function fundSearchFulltextReceive(funds) {
    return {
        type: types.FUND_SEARCH_FULLTEXT_RECEIVE,
        funds
    }
}
