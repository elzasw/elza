/**
 * Akce pro záložky otevřených stromů AP.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vybrání záložky pro strom AP.
 * @param {Object} fa finding aid objekt s informací o verzi
 */
export function selectFaTab(fa) {
    return {
        type: types.FA_SELECT_FA_TAB,
        fa,
    }
}

/**
 * Zavření záložky se stromem AP.
 * @param {Object} fa finding aid objekt s informací o verzi
 */
export function closeFaTab(fa) {
    return {
        type: types.FA_CLOSE_FA_TAB,
        fa
    }
}

