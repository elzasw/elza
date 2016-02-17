/**
 * Akce pro záložky otevřených stromů AP.
 */

import {WebApi} from 'actions'
import {Toastr, i18n} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {faFileTreeFetch} from 'actions/arr/faFileTree'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions'
import {nodesRequest, nodesReceive} from 'actions/arr/node'
import {createFaRoot, getFaFromFaAndVersion} from 'components/arr/ArrUtils'

/**
 * Fetch dat pro otevřené záložky AP, pokud je potřeba - např. název atp.
 */
export function fasFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        var fas = state.arrRegion.fas;

        var versionIds = [];
        fas.forEach(fa => {
            if (fa.dirty && !fa.isFetching) {
                versionIds.push(fa.versionId);
            }
        })

        if (versionIds.length > 0) {
            WebApi.getFindingAidsByVersionIds(versionIds)
                .then(json => {
                    var fas = json.map(x => getFaFromFaAndVersion(x, x.versions[0]))
                    dispatch(fasReceive(fas));

                    // Ještě musíme provést aktualizaci node, pokud je otevřený v záložce takový, který reprezentuje AP - virtuální kořenový NODE
                    fas.forEach(fa => {
                        var node = createFaRoot(fa);
                        dispatch(nodesRequest(fa.versionId, [node.id]));
                        dispatch(nodesReceive(fa.versionId, [node]));
                    })
                })
        }
    }
}

/**
 * Akce - přijata nová data o záložkách AP.
 * @param {Array} fas data
 */
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

/**
 * Vytvoření nové AP.
 * @param {Object} data data fs
 */
export function createFa(data) {
    return dispatch => {
        return WebApi.createFindingAid(data.name, data.ruleSetId, data.rulArrTypeId)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.fa.title.added")));
                dispatch(modalDialogHide());
                dispatch(faFileTreeFetch());
            });
    }
}

/**
 * Uzavření AP, nová aktuální AP bude mít předané ruleSetId a arrangementTypeId.
 * @param {int} versionId verze AP
 * @param {int} ruleSetId id pravidla
 * @param {int} arrangementTypeId id typu výstupu
 */
export function approveFa(versionId, ruleSetId, arrangementTypeId) {
    return dispatch => {
        return WebApi.approveVersion(versionId, ruleSetId, arrangementTypeId)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.fa.title.approved")));
                dispatch(approveFaResult(json.versionId))
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

/**
 * Zapnutí/vypnutí rozšířeného zobrazení stromu AP.
 * {boolean} enable zapnout nebo vypnout rozšířené zobrazení?
 */
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

/**
 * Nová verze AP po jeho schálení.
 * {int} versionId nová verze AP
 */
export function approveFaResult(versionId) {
    return {
        type: types.FA_FA_APPROVE_VERSION,
        versionId: versionId
    }
}