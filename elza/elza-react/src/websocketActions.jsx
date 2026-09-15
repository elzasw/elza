import React from 'react';
import { onReceivedNodeChange } from 'src/websocketController';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import * as daoActions from 'actions/arr/daoActions';
import { store } from 'stores/index.jsx';
import { addToastrDanger, addToastrSuccess } from 'components/shared/toastr/ToastrActions.jsx';
import {} from 'components/shared';
import { FormattedMessage, defineMessages } from "react-intl";

// Id jsou převzatá z legacy katalogu beze změny; sortFailed v katalogu chyběl,
// takže se v toastru vypisovalo "[klíč]".
const messages = defineMessages({
    importSuccess: { id: "ribbon.action.arr.dataGrid.import.success", defaultMessage: "Import byl dokončen" },
    importFailed: { id: "ribbon.action.arr.dataGrid.import.failed", defaultMessage: "Import selhal" },
    sortSuccess: { id: "arr.functions.persistentSort.sortSuccess", defaultMessage: "Seřazení proběhlo úspěšně" },
    sortInterrupted: { id: "arr.functions.persistentSort.sortInterrupted", defaultMessage: "Seřazení bylo přerušeno" },
    sortFailed: { id: "arr.functions.persistentSort.sortFailed", defaultMessage: "Seřazení selhalo" },
    ejSuccess: { id: "arr.functions.computeAndVizualizeEJ.success", defaultMessage: "Výpočet a vizualizace EJ proběhlo úspěšně" },
    ejInterrupted: { id: "arr.functions.computeAndVizualizeEJ.interrupted", defaultMessage: "Výpočet a vizualizace EJ bylo přerušeno" },
    ejError: { id: "arr.functions.computeAndVizualizeEJ.error", defaultMessage: "Výpočet a vizualizace EJ selhalo" },
});

import {
    changeAccessPoint,
    changeAddLevel,
    changeConformityInfo,
    changeDeleteLevel,
    changeFiles,
    changeFund,
    changeFundAction,
    changeFundRecord,
    changeIndexingFinished,
    changeInstitution,
    changeMoveLevel,
    changeNodeRequests,
    changeNodes,
    changeOutputs,
    changePackage,
    changeRegistry,
    changeRequest,
    changeRequestItemQueue,
    changeVisiblePolicy,
    createExtSystem,
    createRequest,
    createRequestItemQueue,
    deleteExtSystem,
    deleteFund,
    deleteRequest,
    fundInvalidChanges,
    fundOutputChanges,
    fundOutputChangesDetail,
    fundOutputStateChange,
    fundOutputStateChangeToastr,
    groupChange,
    groupDelete,
    nodesDelete,
    structureChange,
    updateExtSystem,
    userChange,
} from 'actions/global/change.jsx';

import { reloadUserDetail } from 'actions/user/userDetail';
import { fundVersionApproved } from 'actions/arr/fund.jsx';
import { fundDataGridRefreshRows } from 'actions/arr/fundDataGrid';
import { fundTreeFetch } from 'actions/arr/fundTree';
import { fundTreeInvalidate } from 'actions/arr/fundTree';
import * as types from 'actions/constants/ActionTypes';
import { fundNodeSubNodeFulltextSearch } from 'actions/arr/node';
import { PERSISTENT_SORT_CODE, ZP2015_INTRO_VYPOCET_EJ } from './constants.tsx';
import * as issuesActions from 'actions/arr/issues';
import URLParse from 'url-parse';

import { WebsocketClient } from './websocket/WebsocketClient';


/**
 * Zpracování eventů.
 *
 * @param values {array} seznam příchozí eventů
 */

const serverContextPath = window.serverContextPath;

const url = new URLParse(serverContextPath + '/stomp');

const wsProtocol = url.protocol === 'https:' ? 'wss:' : 'ws:';

export const wsUrl = wsProtocol + '//' + url.host + url.pathname;
console.log('#ws Websocket URL', wsUrl);

let eventMap = {
    DAO_LINK_CREATE: daoLink,
    DAO_LINK_DELETE: daoLink,
    REQUEST_DAO_CHANGE: arrRequest,
    REQUEST_DAO_CREATE: arrRequest,
    CONFORMITY_INFO: conformityInfo,
    INDEXING_FINISHED: indexingFinished,
    PACKAGE: packageEvent,
    INSTITUTION_CHANGE: institutionChange,
    EXTERNAL_SYSTEM_CREATE: extSystemCreate,
    EXTERNAL_SYSTEM_UPDATE: extSystemUpdate,
    EXTERNAL_SYSTEM_DELETE: extSystemDelete,
    NODES_CHANGE: nodesChange,
    OUTPUT_ITEM_CHANGE: outputItemChange,
    FILES_CHANGE: filesChangeEvent,
    BULK_ACTION_STATE_CHANGE: fundActionActionChange,
    DELETE_LEVEL: deleteLevelChange,
    ADD_LEVEL_AFTER: addLevelAfterChange,
    ADD_LEVEL_BEFORE: addLevelBeforeChange,
    ADD_LEVEL_UNDER: addLevelUnderChange,
    APPROVE_VERSION: approveVersionChange,
    MOVE_LEVEL_AFTER: moveLevelAfterChange,
    MOVE_LEVEL_BEFORE: moveLevelBeforeChange,
    MOVE_LEVEL_UNDER: moveLevelUnderChange,
    RECORD_UPDATE: registryChange,
    FUND_UPDATE: fundChange,
    FUND_CREATE: fundChange,
    FUND_RECORD_CHANGE: fundRecordChange,
    VISIBLE_POLICY_CHANGE: visiblePolicyChange,
    FUND_DELETE: fundDelete,
    OUTPUT_STATE_CHANGE: outputStateChange,
    OUTPUT_CHANGES: outputChanges,
    FUND_INVALID: fundInvalid,
    OUTPUT_CHANGES_DETAIL: outputChangesDetail,
    USER_CREATE: changeUser,
    USER_CHANGE: changeUser,
    GROUP_CREATE: changeGroup,
    GROUP_CHANGE: changeGroup,
    GROUP_DELETE: deleteGroup,
    REQUEST_CREATE: requestCreate,
    REQUEST_CHANGE: requestChange,
    REQUEST_DELETE: requestDelete,
    REQUEST_ITEM_QUEUE_CREATE: createRequestItemQueueChange,
    REQUEST_ITEM_QUEUE_DELETE: createRequestItemQueueChange,
    REQUEST_ITEM_QUEUE_CHANGE: changeRequestItemQueueChange,
    DELETE_NODES: deleteNodes,
    FUND_EXTENSION_CHANGE: fundExtensionChange,
    STRUCTURE_DATA_CHANGE: structureDataChange,
    ACCESS_POINT_UPDATE: accessPointUpdate,
    ACCESS_POINT_EXPORT_NEW: () => { },
    ACCESS_POINT_EXPORT_STARTED: () => { },
    ACCESS_POINT_EXPORT_NEED_CONFIRM: () => { },
    ACCESS_POINT_EXPORT_COMPLETED: () => { },
    ACCESS_POINT_EXPORT_FAILED: () => { },
    ISSUE_LIST_UPDATE: issueListUpdate,
    ISSUE_LIST_CREATE: issueListCreate,
    ISSUE_UPDATE: issueUpdate,
    ISSUE_CREATE: issueCreate,
    // Handled by useAiConversation through websocket listeners
    AI_REQUEST_UPDATE: () => { },
    IMPORT_FUND_COMPLETED: importFundCompleted,
    IMPORT_FUND_FAILED: importFundFailed,
};

function importFundCompleted(value) {
    store.dispatch(addToastrSuccess(<FormattedMessage {...messages.importSuccess} />));
    if (value?.versionId) {
        store.dispatch(fundDataGridRefreshRows(value.versionId));
    }
}

function importFundFailed(value) {
    store.dispatch(addToastrDanger(<FormattedMessage {...messages.importFailed} />, value?.message || ''));
}

if (!window.ws) {
    window.ws = new WebsocketClient(wsUrl, eventMap);
    //window.ws.connect();
}

/**
 * Změna uživatele
 * @param value
 */
function changeUser(value) {
    store.dispatch(userChange(value.ids));
    store.dispatch(reloadUserDetail(value.ids));
}
function changeGroup(value) {
    store.dispatch(groupChange(value.ids));
}
function deleteGroup(value) {
    store.dispatch(groupDelete(value.ids[0]));
}

function requestChange(value) {
    store.dispatch(changeRequest(value));
}

function requestDelete(value) {
    store.dispatch(deleteRequest(value));
}

function requestCreate(value) {
    store.dispatch(createRequest(value));
}

function createRequestItemQueueChange(value) {
    store.dispatch(createRequestItemQueue(value));
}

function changeRequestItemQueueChange(value) {
    store.dispatch(changeRequestItemQueue(value));
}

function deleteNodes(value) {
    store.dispatch(nodesDelete(value.versionId, value.entityIds));
}

function fundExtensionChange(value) {
    store.dispatch(changeNodes(value.versionId, [value.nodeId]));
}

function structureDataChange(value) {
    store.dispatch(structureChange(value));
}
function approveVersionChange(value) {
    store.dispatch(fundVersionApproved(value.fundId, value.versionId));
}

function addLevelAfterChange(value) {
    store.dispatch(changeAddLevel(value.versionId, value.node.nodeId, value.staticNodeParent.nodeId));
}

function addLevelBeforeChange(value) {
    store.dispatch(changeAddLevel(value.versionId, value.node.nodeId, value.staticNodeParent.nodeId));
}

function addLevelUnderChange(value) {
    store.dispatch(changeAddLevel(value.versionId, value.node.nodeId, value.staticNode.nodeId));
}

function moveLevelAfterChange(value) {
    store.dispatch(changeMoveLevel(value.versionId));
}

function moveLevelBeforeChange(value) {
    store.dispatch(changeMoveLevel(value.versionId));
}

function moveLevelUnderChange(value) {
    store.dispatch(changeMoveLevel(value.versionId));
}

function registryChange(value) {
    store.dispatch(changeRegistry(value.ids));
}

function fundChange(value) {
    store.dispatch(changeFund(value.ids[0]));
}

function fundDelete(value) {
    store.dispatch(deleteFund(value.ids[0]));
}

function outputStateChange(value) {
    store.dispatch(fundOutputStateChange(value.versionId, value.entityId, value.entityString));
    store.dispatch(fundOutputStateChangeToastr(value.versionId, value.entityId, value.entityString));
}

/*function outputStateChange(value) {
    store.dispatch(fundOutputStateChange(value.versionId, value.entityId));
    store.dispatch(addToastrSuccess('Výstup byl vygenerován.'));
}*/

function outputChanges(value) {
    store.dispatch(fundOutputChanges(value.versionId, value.entityIds));
}

function fundInvalid(value) {
    store.dispatch(fundInvalidChanges(value.fundIds, value.fundVersionIds));
}

function outputChangesDetail(value) {
    store.dispatch(fundOutputChangesDetail(value.versionId, value.entityIds));
}

function fundRecordChange(value) {
    store.dispatch(changeFundRecord(value.versionId, value.nodeId, value.version));
}

function visiblePolicyChange(value) {
    store.dispatch(changeVisiblePolicy(value.versionId, value.nodeIds, value.invalidateNodes));
}

/**
 * Validace uzlu.
 *
 * @param value {object} informace o provedené validace uzlu
 */
function conformityInfo(value) {
    store.dispatch(changeConformityInfo(value.versionId, value.entityIds));
}

/**
 * Změna připojení digitalizátů k JP.
 * @param value objekt
 */
function daoLink(value) {
    store.dispatch(daoActions.changeAllDaos(value.nodeIds));
}

/**
 * Změna požadavků arr request.
 * @param value objekt
 */
function arrRequest(value) {
    store.dispatch(arrRequestActions.changeRequests(value.versionId, value.entityId, value.nodeIds));
    store.dispatch(changeNodeRequests(value.versionId, value.nodeIds));
}

/**
 * Indexace dokončena.
 */
function indexingFinished() {
    store.dispatch(changeIndexingFinished());
}

/**
 * Změna balíčků.
 */
function packageEvent() {
    store.dispatch(changePackage());
}

/**
 * Změna instituce.
 */
function institutionChange() {
    store.dispatch(changeInstitution());
}

function filesChangeEvent(value) {
    store.dispatch(changeFiles(value.versionId, value.entityId));
}

function nodesChange(value) {
    if (!onReceivedNodeChange(value.entityIds)) {
        store.dispatch(changeNodes(value.versionId, value.entityIds));
    }
}
function outputItemChange(value) {
    store.dispatch(changeOutputs(value.versionId, [value.outputId]));
}

function deleteLevelChange(value) {
    store.dispatch(changeDeleteLevel(value.versionId, value.nodeId, value.parentNodeId));
}

/**
 * Změna hromadných akcí.
 */
function fundActionActionChange(value) {
    //speciální handling eventu pro hromadnou akci "PERZISTENTNI_RAZENI"
    if (value.code === PERSISTENT_SORT_CODE) {
        processPersistentSort(value);
    } else if (value.code === ZP2015_INTRO_VYPOCET_EJ) {
        processVisualizeEJ(value);
    }
    store.dispatch(changeFundAction(value.versionId, value.entityId));
}

function processPersistentSort(value) {
    if (value.state === 'FINISHED') {
        const fund = getFund();

        if (fund) {
            store.dispatch(fundTreeInvalidate(fund.versionId))
            //Přenačtení nodeForm
            store.dispatch(fundNodeSubNodeFulltextSearch(undefined));
        }
        store.dispatch(addToastrSuccess(<FormattedMessage {...messages.sortSuccess} />));
    } else if (value.state === 'INTERRUPTED') {
        store.dispatch(addToastrDanger(<FormattedMessage {...messages.sortInterrupted} />));
    } else if (value.state === 'FAILED') {
        store.dispatch(addToastrDanger(<FormattedMessage {...messages.sortFailed} />));
    }
}

function processVisualizeEJ(value) {
    if (value.state === 'FINISHED') {
        const fund = getFund();
        if (fund) {
            store.dispatch(fundTreeFetch(types.FUND_TREE_AREA_MAIN, fund.versionId, null, fund.fundTree.expandedIds));
            //Přenačtení nodeForm
            store.dispatch(fundNodeSubNodeFulltextSearch(undefined));
        }
        store.dispatch(addToastrSuccess(<FormattedMessage {...messages.ejSuccess} />));
    } else if (value.state === 'INTERRUPTED') {
        store.dispatch(addToastrDanger(<FormattedMessage {...messages.ejInterrupted} />));
    } else if (value.state === 'FAILED') {
        store.dispatch(addToastrDanger(<FormattedMessage {...messages.ejError} />));
    }
}

function getFund() {
    let state = store.getState();
    return state.arrRegion.activeIndex != null ? state.arrRegion.funds[state.arrRegion.activeIndex] : null;
}

/**
 * Externí systémy
 */

function extSystemCreate(value) {
    store.dispatch(createExtSystem(value.ids[0]));
}

function extSystemUpdate(value) {
    store.dispatch(updateExtSystem(value.ids[0]));
}

function extSystemDelete(value) {
    store.dispatch(deleteExtSystem(value.ids[0]));
}

function accessPointUpdate(value) {
    store.dispatch(changeAccessPoint(value.ids));
}

function issueListUpdate({ id }) {
    store.dispatch(issuesActions.protocol.invalidate(id));
    store.dispatch(issuesActions.protocols.invalidate());
}

function issueListCreate({ id }) {
    store.dispatch(issuesActions.protocol.invalidate(id));
    store.dispatch(issuesActions.protocols.invalidate());
}

function issueUpdate({ issueListId, ids }) {
    store.dispatch(issuesActions.list.invalidate(issueListId));
    store.dispatch(issuesActions.detail.invalidate(issueListId));
    ids.forEach(id => {
        store.dispatch(issuesActions.comments.invalidate(id));
    });
}

function issueCreate({ issueListId }) {
    store.dispatch(issuesActions.list.invalidate(issueListId));
}

/**
 * Zpracování validací.
 *
 * @param values {array} seznam příchozí validací
 */
function processValidations(values) {
    values.forEach(value => {
        switch (value.validationType) {
            // TODO

            default:
                console.warn('Nedefinovaný typ validace: ' + value.validationType);
                break;
        }
    });
}
