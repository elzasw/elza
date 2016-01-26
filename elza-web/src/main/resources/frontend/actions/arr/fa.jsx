/**
 * Akce pro záložky otevřených stromů AP.
 */

import {WebApi} from 'actions'
import {Toastr, i18n} from 'components';
import * as types from 'actions/constants/actionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {faFileTreeFetch} from 'actions/arr/faFileTree'

export function createFa(data) {
    return dispatch => {
        return WebApi.createFindingAid(data.name, data.ruleSetId, data.rulArrTypeId)
            .then((json) => {
                Toastr.Actions.success({
                    title: i18n("arr.fa.title.added"),
                });                
                dispatch(modalDialogHide())
                dispatch(faFileTreeFetch())
            });
    }
}

export function approveFa(versionId, ruleSetId, arrangementTypeId) {
    return dispatch => {
        return WebApi.approveVersion(versionId, ruleSetId, arrangementTypeId)
            .then((json) => {
                Toastr.Actions.success({
                    title: i18n("arr.fa.title.approved"),
                });                
                dispatch(modalDialogHide())
                dispatch(faFileTreeFetch())
            });
    }
}

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

