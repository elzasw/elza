/**
 * Akce pro digitalizaci.
 */

import * as SimpleListActions from "shared/list/simple/SimpleListActions";
import * as DetailActions from "shared/detail/DetailActions";
import {WebApi} from 'actions/index.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {i18n} from 'components/index.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'

const AREA_PREPARED_DIGITIZATION_REQUESTS = "preparedDigitizationRequestList";
const AREA_DIGITIZATION_REQUEST_LIST_SUFFIX = ".requestList";
const AREA_DIGITIZATION_REQUEST_DETAIL_SUFFIX = ".requestDetail";

/**
 * Načtení seznamu NEODESLANÝCH požadavků na digitalizaci.
 * @param versionId verze AS
 * @return {function(*, *)}
 */
export function fetchPreparedListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded(AREA_PREPARED_DIGITIZATION_REQUESTS, versionId, (parent, filter) => {
        return WebApi.getDigitizationRequests(versionId, "OPEN")
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Načtení požadavků na digitalizaci pro konkrétní fond.
 * @param versionId verze AS
 */
export function fetchListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DIGITIZATION_REQUEST_LIST_SUFFIX, versionId, (parent, filter) => {
        return WebApi.getArrRequests(versionId)
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Načtení detailu požadavku na digitalizaci pro konkrétní fond.
 * @param versionId verze AS
 * @param id id požadavku
 */
export function fetchDetailIfNeeded(versionId, id) {
    return DetailActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_DIGITIZATION_REQUEST_DETAIL_SUFFIX, id, (id) => {
        return WebApi.getArrRequest(versionId, id);
    });
}

/**
 * Výběr nového detailu požadavku na digitalizaci.
 * @param versionId verze AS
 * @param id id požadavku
 */
export function selectDetail(versionId, id) {
    return DetailActions.select("fund[" + versionId + "]" + AREA_DIGITIZATION_REQUEST_DETAIL_SUFFIX, id);
}

/**
 * Uložení požadavku.
 * @param versionId verze AS
 * @param id id požadavku
 * @param data upravená data
 * @return {function(*=, *)}
 */
export function requestEdit(versionId, id, data) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.updateArrRequest(versionId, id, data));
    }
}

/**
 * Přidání JP do požadavku.
 * @param versionId verze AS
 * @param id id požadavku
 * @param nodeIds seznam id node pro přidání
 */
export function addNodes(versionId, id, nodeIds) {
    return (dispatch, getState) => {
        WebApi.arrRequestAddNodes(versionId, id, nodeIds)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.request.title.nodesAdded")));
                dispatch(modalDialogHide());
            });
    }
}

/**
 * Odebrání JP od požadavku.
 * @param versionId verze AS
 * @param id id požadavku
 * @param nodeId id node pro odebrání
 */
export function removeNode(versionId, id, nodeId) {
    return (dispatch, getState) => {
        WebApi.arrRequestRemoveNodes(versionId, id, [nodeId])
    }
}