/**
 * Akce pro vyhledávání archivních souborů v komponentě Modal
 */
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

export function isFundModalAction(action) {
    switch (action.type) {
        case types.FUND_FUND_MODAL_FULLTEXT_CHANGE:
        case types.FUND_FUND_MODAL_FULLTEXT_RESULT:
        case types.FUND_FUND_MODAL_EXPAND_NODE:
            return true
        default:
            return false
    }
}

/**
 * Hledání v archivních souborech
 * @param {string} filterText hledaný text
 */
export function fundModalFulltextChange(filterText) {
    return {
        type: types.FUND_FUND_MODAL_FULLTEXT_CHANGE,
        filterText
    }
}

/**
 * Výsledek hledání v archivních souborech
 * @param {string} filterText - pro jaký hledaný text platí výsledky
 * @param {Array} searchedData - seznam nalezených node
 * @param {boolean} clearFilter - jedná se o akci, která má pouze vymazat aktuální filtr?
 */
function fundModalFulltextResult(filterText, searchedData, clearFilter) {
    return {
        type: types.FUND_FUND_TREE_FULLTEXT_RESULT,
        filterText,
        searchedData,
        clearFilter
    }
}

/**
 * Akce fulltextového hledání v archivních souborech
 * @param {string} filterText - podle jakého řetězce se má vyhledávat
 */
export function fundModalFulltextSearch(filterText) {
    return (dispatch, getState) => {
        if (filterText.length > 0) {
            return WebApi.fundFulltext(filterText)
                .then(json => {
                    dispatch(fundModalFulltextResult(filterText, json, false))
                });
        } else {
            return dispatch(fundModalFulltextResult(filterText, [], true))
        }
    }
}
