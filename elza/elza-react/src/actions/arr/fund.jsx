/**
 * Akce pro záložky otevřených stromů AS.
 */

import {WebApi} from 'actions/index.jsx';
import {Api} from "../../api";
import {i18n} from 'components/shared';
import * as types from 'actions/constants/ActionTypes';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {nodesReceive, nodesRequest} from 'actions/arr/node.jsx';
import {createFundRoot, getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx';
import {fundsSelectFund} from 'actions/fund/fund.jsx';
import {changeApproveVersion} from 'actions/global/change.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {storeLoadData} from 'actions/store/store.jsx';
import {downloadExportFile} from '../global/downloadExportFile';

/**
 * Fetch dat pro otevřené záložky AS, pokud je potřeba - např. název atp.
 */
export function fundsFetchIfNeeded() {
    return (dispatch, getState) => {
        const state = getState();
        const funds = state.arrRegion.funds;

        const versionIds = [];
        funds.forEach(fund => {
            if (fund.dirty && !fund.isFetching) {
                versionIds.push(fund.versionId);
            }
        });

        if (versionIds.length > 0) {
            dispatch(fundsRequest(versionIds));
            WebApi.getFundsByVersionIds(versionIds).then(json => {
                const funds = json.map(x => getFundFromFundAndVersion(x, x.versions[0]));
                dispatch(fundsReceive(funds));

                // Ještě musíme provést aktualizaci node, pokud je otevřený v záložce takový, který reprezentuje AS - virtuální kořenový NODE
                funds.forEach(fund => {
                    const node = createFundRoot(fund);
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
        ruleSetCode: data.ruleSetCode,
        internalCode: data.internalCode,
        institutionIdentifier: data.institutionIdentifier,
        dateRange: data.dateRange,
        adminUsers: [],
        adminGroups: [],
        fundNumber: data.fundNumber,
        unitdate:data.unitdate,
        mark: data.mark,
        scopes: data.scopes,
    };

    data.fundAdmins &&
        data.fundAdmins.forEach(i => {
            if (i.user) {
                formData.adminUsers.push(i.user.id);
            } else if (i.group) {
                formData.adminGroups.push(i.group.id);
            }
        });

    return dispatch => {
        return savingApiWrapper(dispatch, Api.funds.fundCreateFund(formData)).then(response => {
            dispatch(addToastrSuccess(i18n('arr.fund.title.added')));
            dispatch(fundsSelectFund(response.data.id));
        });
    };
}

export function updateFund(id, data) {
    return dispatch => {
        return savingApiWrapper(dispatch, Api.funds.fundUpdateFund(id, data));
    };
}

/**
 * Uzavření AS, nová aktuální AS bude mít předané ruleSetId a arrangementTypeId.
 * @param {int} versionId verze AS
 */
export function approveFund(versionId) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.approveVersion(versionId)).then(json => {
            dispatch(addToastrSuccess(i18n('arr.fund.title.approved')));
            dispatch(approveFundResult(versionId, json));
        });
    };
}

/**
 * Handle the version-approval event for open fund tabs.
 *
 * A tab open on the approved version transitions to the new open version:
 * their content is identical at the approval instant, and all subsequent
 * change events carry the new versionId, so a tab left on the closed
 * version would silently stop receiving updates. The plain
 * changeApproveVersion action is dispatched first, so the tab is marked
 * closed immediately (and stays safely closed if the fetch below fails).
 *
 * @param {int} fundId id AS
 * @param {int} versionId schválená (uzavřená) verze AS
 */
export function fundVersionApproved(fundId, versionId) {
    return async (dispatch, getState) => {
        dispatch(changeApproveVersion(fundId, versionId));

        const {arrRegion} = getState();
        if (!arrRegion.funds.some(fund => fund.versionId === versionId)) {
            return;
        }

        try {
            const fund = await WebApi.getFundDetail(fundId);
            const version = fund.versions.find(v => !v.lockDate);
            if (version) {
                dispatch(approveFundResult(versionId, version));
            }
        } catch (e) {
            console.error('Nepodařilo se načíst novou verzi AS po schválení', fundId, e);
        }
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

// Export fondu, využití metod na získání id export requestu, zjistění stavu a adresy pro stažení
export function exportFund(fundId, { exportFilter, includeUUID, includeAccessPoints, includeDaos }) {
    let requestData = {
        fundsSections: [{fundVersionId: fundId}],
        exportFilter,
        includeUUID,
        includeAccessPoints,
        includeDaos,
    };

    return async (dispatch) => {
        const { data: fileId } = await Api.io.ioExportRequest(requestData);
        dispatch(downloadExportFile(fileId));
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
            dispatch(storeLoadData('ARR_REGION_FUND', itemFound));
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
 * Přechod záložky AS na novou otevřenou verzi po schválení.
 * @param {int} versionId schválená (uzavřená) verze AS, podle ní se dohledá záložka
 * @param {Object} version nová otevřená verze AS
 */
export function approveFundResult(versionId, version) {
    return {
        type: types.FUND_FUND_APPROVE_VERSION,
        versionId,
        version,
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
