/**
 * Akce pro záložky otevřených stromů AS.
 */

import {UrlFactory, WebApi} from 'actions/index.jsx';
import {i18n} from 'components/shared';
import * as types from 'actions/constants/ActionTypes.js';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {nodesReceive, nodesRequest} from 'actions/arr/node.jsx';
import {createFundRoot, getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx';
import {fundsSelectFund} from 'actions/fund/fund.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {storeLoadData} from 'actions/store/store.jsx';
import {downloadAjaxFile} from '../global/download';

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
        });

        if (versionIds.length > 0) {
            dispatch(fundsRequest(versionIds));
            WebApi.getFundsByVersionIds(versionIds).then(json => {
                var funds = json.map(x => getFundFromFundAndVersion(x, x.versions[0]));
                dispatch(fundsReceive(funds));

                // Ještě musíme provést aktualizaci node, pokud je otevřený v záložce takový, který reprezentuje AS - virtuální kořenový NODE
                funds.forEach(fund => {
                    var node = createFundRoot(fund);
                    dispatch(nodesRequest(fund.versionId, [node.id]));
                    dispatch(nodesReceive(fund.versionId, [node]));
                });
            });
        }
    };
}

export function fundsRequest(versionIds) {
    let fundMap = {};
    versionIds.forEach(id => {
        fundMap[id] = true;
    });
    return {
        type: types.FUND_FUNDS_REQUEST,
        fundMap,
    };
}

/**
 * Akce - přijata nová data o záložkách AS.
 * @param {Array} funds data
 */
export function fundsReceive(funds) {
    var fundMap = {};
    funds.forEach(fund => {
        fundMap[fund.versionId] = fund;
    });

    return {
        type: types.FUND_FUNDS_RECEIVE,
        funds,
        fundMap,
    };
}

/**
 * Vytvoření nové AS.
 * @param {Object} data data
 */
export function createFund(data) {
    const formData = {
        name: data.name,
        ruleSetId: data.ruleSetId,
        internalCode: data.internalCode,
        institutionId: data.institutionId,
        dateRange: data.dateRange,
        adminUsers: [],
        adminGroups: [],
    };

    data.fundAdmins &&
        data.fundAdmins.forEach(i => {
            if (i.user) {
                formData.adminUsers.push(i.user);
            } else if (i.group) {
                formData.adminGroups.push(i.group);
            }
        });

    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.createFund(formData)).then(fund => {
            dispatch(addToastrSuccess(i18n('arr.fund.title.added')));
            dispatch(fundsSelectFund(fund.id));
        });
    };
}

export function updateFund(data) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.updateFund(data));
    };
}

/**
 * Uzavření AS, nová aktuální AS bude mít předané ruleSetId a arrangementTypeId.
 * @param {int} versionId verze AS
 * @param {int} dateRange
 */
export function approveFund(versionId, dateRange) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.approveVersion(versionId, dateRange)).then(json => {
            dispatch(addToastrSuccess(i18n('arr.fund.title.approved')));
            dispatch(approveFundResult(json.versionId));
        });
    };
}

export function deleteFund(fundId) {
    return dispatch => {
        return WebApi.deleteFund(fundId);
    };
}

export function deleteFundHistory(fundId) {
    return dispatch => {
        return WebApi.deleteFundHistory(fundId);
    };
}

export function exportFund(fundId, transformationName) {
    let requestData = {
        fundsSections: [{fundVersionId: fundId}],
        transformationName: transformationName,
    };
    return dispatch => {
        dispatch(downloadAjaxFile(UrlFactory.exportFund(), 'elza-data.xml', 'POST', requestData));
    };
}

/**
 * Vybrání záložky pro strom AS. Pokud již AS byla dříva otevřena, použije se nastavení z tohoto otevření - obdobně jako otevřením AS z home stránky.
 * @param {Object} fund finding aid objekt s informací o verzi
 */
export function selectFundTab(fund) {
    return (dispatch, getState) => {
        // Dohledání dříve otevřeného fundo v konkrétní verzi
        var itemFound = null;
        const state = getState();
        const {
            stateRegion: {arrRegionFront},
        } = state;
        for (let a = 0; a < arrRegionFront.length; a++) {
            const item = arrRegionFront[a];
            if (item.id === fund.id && item.versionId === fund.versionId) {
                itemFound = item;
                break;
            }
        }
        if (itemFound) {
            dispatch(storeLoadData('ARR_REGION_FUND', itemFound, true));
        } else {
            dispatch({
                type: types.FUND_SELECT_FUND_TAB,
                fund,
            });
        }
    };
}

/**
 * Zavření záložky se stromem AS.
 * @param {Object} fund finding aid objekt s informací o verzi
 */
export function closeFundTab(fund) {
    return {
        type: types.FUND_CLOSE_FUND_TAB,
        fund,
    };
}

export function showRegisterJp(show) {
    return {
        type: types.SHOW_REGISTER_JP,
        showRegisterJp: show,
    };
}

/**
 * Nová verze AS po jeho schálení.
 * {int} versionId nová verze AS
 */
export function approveFundResult(versionId) {
    return {
        type: types.FUND_FUND_APPROVE_VERSION,
        versionId: versionId,
    };
}

/**
 * Informační zpráva při změně read mode pro fund.
 * @param versionId verze AS
 * @param readMode nový stav read mode
 */
export function fundChangeReadMode(versionId, readMode) {
    return {
        type: types.FUND_FUND_CHANGE_READ_MODE,
        versionId,
        readMode,
    };
}
