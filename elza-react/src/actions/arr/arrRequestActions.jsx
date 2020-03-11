/**
 * Akce pro digitalizaci.
 */

import * as SimpleListActions from 'shared/list/simple/SimpleListActions';
import * as DetailActions from 'shared/detail/DetailActions';
import {WebApi} from 'actions/index.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {i18n} from 'components/shared';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';
import {storeFromArea} from 'shared/utils';

const AREA_PREPARED_REQUESTS = "preparedRequestList";
const AREA_REQUEST_LIST_SUFFIX = ".requestList";
const AREA_REQUEST_IN_QUEUE_LIST = "requestInQueueList";
const AREA_REQUEST_DETAIL_SUFFIX = ".requestDetail";

/**
 * Načtení seznamu NEODESLANÝCH požadavků.
 * @param versionId verze AS
 * @param type typ požadavků
 * @return {function(*, *)}
 */
export function fetchPreparedListIfNeeded(versionId, type) {
    return SimpleListActions.fetchIfNeeded(AREA_PREPARED_REQUESTS, { versionId, type }, (parent, filter) => {
        return WebApi.findRequests(versionId, parent.type, "OPEN")
            .then(json => ({rows: json, count: 0}))
            .then(data => {
                if (parent.type === "DAO") {
                    return {
                        count: 0,
                        rows: data.rows.filter(i => i.type === filter.daoType)
                    }
                } else {
                    return data;
                }
            });
    });
}

/**
 * Filtrování seznamu NEODESLANÝCH požadavků.
 * @param versionId verze AS
 * @param filter
 */
export function filterPreparedList(filter) {
    return SimpleListActions.filter(AREA_PREPARED_REQUESTS, filter);
}

/**
 * Invalidace NEODESLANÝCH požadavků.
 */
export function preparedListInvalidate() {
    return SimpleListActions.invalidate(AREA_PREPARED_REQUESTS, null);
}

/**
 * Odeslání požadavku.
 * @param versionId verze AS
 * @param versionId verze AS
 * @param versionId verze AS
 * @param requestId id požadavku
 * @return {function(*, *)}
 */
export function sendRequest(versionId, requestId) {
    return (dispatch, getState) => {
        return WebApi.sendArrRequest(versionId, requestId)
    }
}

/**
 * Smazání požadavku.
 * @param versionId verze AS
 * Smazání požadavku.
 * @return {function(*, *)}
 */
export function deleteRequest(versionId, requestId) {
    return (dispatch, getState) => {
        return WebApi.deleteArrRequest(versionId, requestId)
    }
}

/**
 * Načtení požadavků pro daný AS.
 * @param versionId verze AS
 */
export function fetchListIfNeeded(versionId) {
    return SimpleListActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_REQUEST_LIST_SUFFIX, versionId, (parent, filter) => {
        let type;
        let subType = null;
        if (filter.type === 'DESTRUCTION' || filter.type === 'TRANSFER') {
            type = 'DAO';
            subType = filter.type;
        } else {
            type = filter.type;
        }
        return WebApi.findRequests(versionId, type, null, filter.description, filter.fromDate, filter.toDate, subType)
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Filtrování požadavků pro daný AS.
 * @param versionId verze AS
 * @param filter
 */
export function filterList(versionId, filter) {
    return SimpleListActions.filter("fund[" + versionId + "]" + AREA_REQUEST_LIST_SUFFIX, filter);
}

/**
 * Zpráva informující o změně požadavků pro konkrétní AS.
 * @param versionId verze AS
 * @param reqId id požadavku
 * @param nodeIds seznam node, se kterými se něco dělalo (např. byly přidány nebo odebrány)
 */
export function changeRequests(versionId, reqId, nodeIds) {
    return (dispatch, getState) => {
        // Seznam požadavků
        dispatch(SimpleListActions.invalidate("fund[" + versionId + "]" + AREA_REQUEST_LIST_SUFFIX, null));

        // Seznam otevřených požadavků pro přidání na pořádání
        dispatch(SimpleListActions.invalidate(AREA_PREPARED_REQUESTS, null));

        // Detail požadavku
        const detailArea = "fund[" + versionId + "]" + AREA_REQUEST_DETAIL_SUFFIX;
        const requestDetail = storeFromArea(getState(), detailArea);
        if (requestDetail.id === reqId) {
            dispatch(DetailActions.invalidate(detailArea, null));
        }
    }
}

/**
 * Invalidace požadavků.
 */
export function listInvalidate(versionId) {
    return SimpleListActions.invalidate("fund[" + versionId + "]" + AREA_REQUEST_LIST_SUFFIX, null);
}

/**
 * Načtení všech požadavků ve frontě.
 */
export function fetchInQueueListIfNeeded() {
    return SimpleListActions.fetchIfNeeded(AREA_REQUEST_IN_QUEUE_LIST, null, (parent, filter) => {
        return WebApi.getRequestsInQueue()
            .then(json => ({rows: json, count: 0}));
    });
}

/**
 * Invalidace požadavků ve frontě.
 */
export function queueListInvalidate() {
    return SimpleListActions.invalidate(AREA_REQUEST_IN_QUEUE_LIST, null);
}

/**
 * Načtení detailu požadavku pro konkrétní fond.
 * @param versionId verze AS
 * @param id id požadavku
 */
export function fetchDetailIfNeeded(versionId, id) {
    return DetailActions.fetchIfNeeded("fund[" + versionId + "]" + AREA_REQUEST_DETAIL_SUFFIX, id, (id) => {
        return WebApi.getArrRequest(versionId, id);
    });
}

/**
 * Výběr nového detailu požadavku.
 * @param versionId verze AS
 * @param id id požadavku
 */
export function selectDetail(versionId, id) {
    return DetailActions.select("fund[" + versionId + "]" + AREA_REQUEST_DETAIL_SUFFIX, id);
}

/**
 * Zrušení detailu výběru požadavku, pokud je požadavek daného id zobrazen v detailu.
 * @param versionId verze AS
 * @param id jaké id se má testovat - pokud je zobrazen v detailu požadavek s tímto id, bude detail zrušen - nebude vybrán žádný požýadavek
 * @return {function(*, *)}
 */
export function detailUnselect(versionId, id) {
    return (dispatch, getState) => {
        const area = "fund[" + versionId + "]" + AREA_REQUEST_DETAIL_SUFFIX;
        const detailStore = storeFromArea(getState(), area);
        if (detailStore.id === id) {
            dispatch(DetailActions.select(area, null));
        }
    };
}

/**
 * Invalidace dat požadavku.
 * @param versionId verze AS
 * @param id id požadavku
 * @return {*}
 */
export function detailInvalidate(versionId, id) {
    return DetailActions.invalidate("fund[" + versionId + "]" + AREA_REQUEST_DETAIL_SUFFIX, id)
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
 * Přidání JP do požadavku na digitalizaci.
 * @param versionId verze AS
 * @param request požadavek
 * @param nodeIds seznam id node pro přidání
 */
export function addNodes(versionId, request, nodeIds, digitizationFrontdeskId) {
    return (dispatch, getState) => {
        WebApi.arrDigitizationRequestAddNodes(versionId, request.id, false, request.description, nodeIds, digitizationFrontdeskId)
            .then((json) => {
                dispatch(addToastrSuccess(i18n("arr.request.title.nodesAdded")));
                dispatch(modalDialogHide());
            });
    }
}

/**
 * Odebrání JP od požadavkuna digitalizaci.
 * @param versionId verze AS
 * @param request požadavek
 * @param nodeId id node pro odebrání
 */
export function removeNode(versionId, request, nodeId) {
    return (dispatch, getState) => {
        WebApi.arrRequestRemoveNodes(versionId, request.id, [nodeId])
    }
}
