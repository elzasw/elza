/**
 * Akce pro záložky otevřených stromů AS.
 */

import {WebApi} from 'actions'
import {Toastr, i18n} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {modalDialogHide} from 'actions/global/modalDialog'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions'
import {nodesRequest, nodesReceive} from 'actions/arr/node'
import {createFundRoot, getFundFromFundAndVersion} from 'components/arr/ArrUtils'

/**
 * Fetch dat pro otevřené záložky AS, pokud je potřeba - např. název atp.
 */
export function fundsFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        var funds = state.arrRegion.funds;

        var versionIds = [];
        funds.forEach(fund => {
            if (fund.dirty && !fund.isFetching) {
                versionIds.push(fund.versionId);
            }
        })

        if (versionIds.length > 0) {
            WebApi.getFundsByVersionIds(versionIds)
                .then(json => {
                    var funds = json.map(x => getFundFromFundAndVersion(x, x.versions[0]))
                    dispatch(fundsReceive(funds));

                    // Ještě musíme provést aktualizaci node, pokud je otevřený v záložce takový, který reprezentuje AS - virtuální kořenový NODE
                    funds.forEach(fund => {
                        var node = createFundRoot(fund);
                        dispatch(nodesRequest(fund.versionId, [node.id]));
                        dispatch(nodesReceive(fund.versionId, [node]));
                    })
                })
        }
    }
}

/**
 * Akce - přijata nová data o záložkách AS.
 * @param {Array} funds data
 */
export function fundsReceive(funds) {
    var fundMap = {}
    funds.forEach(fund => {
        fundMap[fund.versionId] = fund
    })

    return {
        type: types.FUND_FUNDS_RECEIVE,
        funds,
        fundMap
    }
}

/**
 * Vytvoření nové AS.
 * @param {Object} data data
 */
export function createFund(data) {
    return dispatch => {
        return WebApi.createFund(data.name, data.ruleSetId, data.institutionId, data.internalCode, data.dateRange)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.fund.title.added")));
                dispatch(modalDialogHide());
            });
    }
}

/**
 * Uzavření AS, nová aktuální AS bude mít předané ruleSetId a arrangementTypeId.
 * @param {int} versionId verze AS
 * @param {int} arrangementTypeId id typu výstupu
 */
export function approveFund(versionId, dateRange) {
    return dispatch => {
        return WebApi.approveVersion(versionId, dateRange)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.fund.title.approved")));
                dispatch(approveFundResult(json.versionId))
                dispatch(modalDialogHide())
            });
    }
}

/**
 * Vybrání záložky pro strom AS.
 * @param {Object} fund finding aid objekt s informací o verzi
 */
export function selectFundTab(fund) {
    return {
        type: types.FUND_SELECT_FUND_TAB,
        fund,
    }
}

/**
 * Zavření záložky se stromem AS.
 * @param {Object} fund finding aid objekt s informací o verzi
 */
export function closeFundTab(fund) {
    return {
        type: types.FUND_CLOSE_FUND_TAB,
        fund
    }
}

/**
 * Zapnutí/vypnutí rozšířeného zobrazení stromu AS.
 * {boolean} enable zapnout nebo vypnout rozšířené zobrazení?
 */
export function fundExtendedView(enable) {
    return {
        type: types.FUND_EXTENDED_VIEW,
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
 * Nová verze AS po jeho schálení.
 * {int} versionId nová verze AS
 */
export function approveFundResult(versionId) {
    return {
        type: types.FUND_FUND_APPROVE_VERSION,
        versionId: versionId
    }
}