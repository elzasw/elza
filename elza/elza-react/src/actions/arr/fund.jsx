/**
 * Akce pro záložky otevřených stromů AS.
 */

import {WebApi} from 'actions/index.jsx';
import {Api, getFullPath} from "../../api";
import {i18n} from 'components/shared';
import * as types from 'actions/constants/ActionTypes';
import {addToastrInfo,addToastrSuccess, removeToastr} from 'components/shared/toastr/ToastrActions.jsx';
import {nodesReceive, nodesRequest} from 'actions/arr/node.jsx';
import {createException} from 'components/ExceptionUtils';
import {createFundRoot, getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx';
import {fundsSelectFund} from 'actions/fund/fund.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {storeLoadData} from 'actions/store/store.jsx';
import {downloadFile} from '../global/download';
import {ExportRequestState, IoApiAxiosParamCreator} from 'elza-api';

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
export function exportFund(fundId, { exportFilterId, includeUUID, includeAccessPoints }) {
    let requestData = {
        fundsSections: [{fundVersionId: fundId}],
        exportFilterId,
        includeUUID,
        includeAccessPoints,
    };

    // opakovane dotazovani na stav exportu, konci stazenim souboru ci hlaskou o neuspechu
    function downloadExportFile (fileId, interval = 4000, toastKey = undefined) {
        return async (dispatch, getState) => {
            // toastKey obsahuje key posledne vytvoreneho toastu, coz je info toast o generovani
            if (!toastKey) {
                // pokud toastKey neni predan, vytvorim info toast
                dispatch(addToastrInfo(i18n('export.generating'), undefined, undefined, null));
                const { toastr } = getState();
                // ziskani lastKey ze statu, pomoci nehoz toast odstranime pri (ne)uspechu exportu
                toastKey = toastr.lastKey;
            }
            try {
                // ziskani stavu exportu, overrideErrorHandler: true pro zabraneni vychoziho zobrazeni chybove hlasky
                const { data } = await Api.io.ioGetExportStatus(fileId, { overrideErrorHandler: true });
                // pri stavu "Finished" muzeme soubor stahnout
                if (data.state === ExportRequestState.Finished) {
                    // odstraneni info toastu pomoci toastKey o generovani exportu
                    dispatch(removeToastr(toastKey));
                    // hlaska o uspesnem exportu
                    dispatch(addToastrSuccess(i18n('export.success'), undefined, undefined, 4000));
                    // ziskani cesty k souboru
                    const { url } = await IoApiAxiosParamCreator().ioGetExportFile(fileId);
                    // stazeni souboru
                    dispatch(downloadFile(getFullPath(url)));
                } else {
                    // pri jinych stavech (PENDING/PREPARING) - opetovne zavolani funkce s danym intervalem
                    setTimeout(() => dispatch(downloadExportFile(fileId, interval, toastKey)), interval);
                }
            } catch (error) {
                // pri chybe/neuspechu odstranim info toast a vypisi informace o chybe
                const code = error.response.data.code;
                dispatch(removeToastr(toastKey));
                dispatch(
                    createException({
                        ...error.response.data,
                        code: code === 'CANT_EXPORT_DELETED_AP' ? 'CANT_EXPORT_DELETED_AP' : 'GENERATING_EXPORT_FAILED',
                    }),
                );
            }
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
