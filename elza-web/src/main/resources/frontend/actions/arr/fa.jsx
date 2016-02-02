/**
 * Akce pro záložky otevřených stromů AP.
 */

import {WebApi} from 'actions'
import {Toastr, i18n} from 'components';
import * as types from 'actions/constants/actionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {faFileTreeFetch} from 'actions/arr/faFileTree'
import {getFaFromFaAndVersion} from 'components/arr/ArrUtils'

export function fasFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        var fas = state.arrRegion.fas;

        var versionIds = [];
        fas.forEach(fa => {
            if (fa.dirty) {
                versionIds.push(fa.versionId);
            }
        })

        if (versionIds.length > 0) {
            WebApi.getFindingAidsByVersionIds(versionIds)
                .then(json => {
                    var fas = json.map(x => getFaFromFaAndVersion(x, x.versions[0]))
                    dispatch(fasReceive(fas));
                })
        }
    }
}

export function fasReceive(fas) {
    var faMap = {}
    fas.forEach(fa => {
        faMap[fa.versionId] = fa
    })

    return {
        type: types.FA_FAS_RECEIVE,
        fas,
        faMap
    }
}

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

export function faExtendedView(enable) {
    return {
        type: types.FA_EXTENDED_VIEW,
        enable
    }
}

export function showRegisterJp(show) {
    return {
        type: types.SHOW_REGISTER_JP,
        showRegisterJp: show
    }
}