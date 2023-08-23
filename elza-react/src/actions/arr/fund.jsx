/**
 * Akce pro záložky otevřených stromů AS.
 */

import {UrlFactory, WebApi} from 'actions/index.jsx';
import {Api, getFullPath} from "../../api";
import {i18n} from 'components/shared';
import * as types from 'actions/constants/ActionTypes';
import {addToastrDanger, addToastrInfo,addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {nodesReceive, nodesRequest} from 'actions/arr/node.jsx';
import {createFundRoot, getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx';
import {fundsSelectFund} from 'actions/fund/fund.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {storeLoadData} from 'actions/store/store.jsx';
import {downloadFile} from '../global/download';
import { IoApiAxiosParamCreator } from 'elza-api';

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
        return savingApiWrapper(dispatch, Api.funds.createFund(formData)).then(response => {
            dispatch(addToastrSuccess(i18n('arr.fund.title.added')));
            dispatch(fundsSelectFund(response.data.id));
        });
    };
}

export function updateFund(id, data) {
    return dispatch => {
        return savingApiWrapper(dispatch, Api.funds.updateFund(id, data));
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

// Export fondu, využití metod na získání id export requestu, zjistění stavu a adresy pro stažení
export function exportFund(fundId, transformationName, exportFilterId) {
    let requestData = {
        fundsSections: [{fundVersionId: fundId}],
        exportFilterId: exportFilterId,
    };

    // opakovane dotazovani na stav exportu, konci stazenim souboru ci hlaskou o neuspechu
    function downloadExportFile (fileId, interval = 4000) {
        return async (dispatch) => {
            dispatch(addToastrInfo(i18n('export.generating'), undefined, undefined, interval));
            const isPending = await checkExportStatus(fileId);
            if (isPending === false) {
                dispatch(addToastrSuccess(i18n('export.success'), undefined, undefined, 4000));
                // ziskani cesty k souboru
                const { url } = await IoApiAxiosParamCreator().ioGetExportFile(fileId);
                // stazeni souboru 
                dispatch(downloadFile(getFullPath(url)));
            } else if (isPending === true) {
                // opetovne zavolani funkce s danym intervalem
                setTimeout(() => downloadExportFile(fileId, interval), interval);
            }
        }
    }

    // zjisteni stavu exportu, navratova hodnota boolean (isPending) nebo null pri negativni odpovedi
    async function checkExportStatus (fileId) {
        try {
            const st = await Api.io.ioGetExportStatus(fileId);
            const { status, data } = st;
            if (status === 200 && data.state==='FINISHED' ) {
                return false;
            } else {
                return true;
            }
        } catch (error) {
            return null;
        };
    }

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
