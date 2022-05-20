import React from 'react';
import {webSocketConnect, webSocketDisconnect} from 'actions/global/webSocket.jsx';
import {onReceivedNodeChange} from 'websocketController.jsx';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import * as daoActions from 'actions/arr/daoActions';
import {store} from 'stores/index.jsx';
import {addToastrDanger, addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {i18n} from 'components/shared';
import {checkUserLogged} from 'actions/global/login.jsx';

import {
    changeAccessPoint,
    changeAddLevel,
    changeApproveVersion,
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

import {Stomp} from 'stompjs';
import URLParse from 'url-parse';

import {reloadUserDetail} from 'actions/user/userDetail';
import {fundTreeFetch} from 'actions/arr/fundTree';
import {fundTreeInvalidate} from 'actions/arr/fundTree';
import * as types from 'actions/constants/ActionTypes';
import {fundNodeSubNodeFulltextSearch} from 'actions/arr/node';
import {PERSISTENT_SORT_CODE, ZP2015_INTRO_VYPOCET_EJ} from './constants.tsx';
import * as issuesActions from 'actions/arr/issues';

const serverContextPath = window.serverContextPath;

const url = new URLParse(serverContextPath + '/stomp');

const wsProtocol = url.protocol === 'https:' ? 'wss:' : 'ws:';

const wsUrl = wsProtocol + '//' + url.host + url.pathname;
console.log('Websocekt URL', wsUrl);

class websocket {
    constructor(url, eventMap) {
        this.nextReceiptId = 0;
        this.pendingRequests = {};
        this.stompClient = null;
        this.url = url;
        this.eventMap = eventMap;
    }

    connect = (heartbeatOut = 20000, heartbeatIn = 20000) => {
        if (Stomp) {
            this.stompClient = Stomp.client(this.url);
            this.stompClient.debug = null;
            this.stompClient.heartbeat.outgoing = heartbeatOut;
            this.stompClient.heartbeat.incoming = heartbeatIn;
            this.stompClient.onreceipt = this.onReceipt;
            this.stompClient.onerror = this.onError; // Napodobeni chovani z vyssi verze
            console.info('Websocket connecting to ' + url);
            this.stompClient.connect({}, this.onConnect, this.onError);
        }
    };

    disconnect = (error = false) => {
        if (this.stompClient && this.stompClient.ws.readyState < 3) {
            // When ready state is not CLOSING(2) or CLOSED(3) and stompClient exists
            console.log('Websocket disconnected');
            this.stompClient.disconnect();
            this.stompClient = null;
        }
        store.dispatch(webSocketDisconnect(error));
    };

    reconnect = () => {
        this.disconnect();
        this.connect();
    };

    send = (url, data, onSuccess, onError) => {
        const headers = {};

        if (onSuccess || onError) {
            headers.receipt = this.nextReceiptId;

            let nextRequest = {
                url: url,
                headers: headers,
                data: data,
                onSuccess: onSuccess,
                onError: onError,
            };

            this.pendingRequests[this.nextReceiptId] = nextRequest;
            this.nextReceiptId++;
        }

        this.stompClient.send(url, headers, data);
    };

    onConnect = frame => {
        store.dispatch(webSocketConnect());
        console.info('Websocket connected');
        this.stompClient.subscribe('/topic/api/changes', this.onMessage);
    };

    onError = error => {
        const {body, headers, command} = error;

        store.dispatch(
            checkUserLogged(logged => {
                if (logged) {
                    if (command === 'ERROR' && headers) {
                        // Error message received from server

                        this.disconnect(true);
                        let message = headers.message || '';

                        if (body) {
                            const bodyObj = JSON.parse(body);
                            message = bodyObj.message;
                        }

                        store.dispatch(addToastrDanger(i18n('global.error.ws'), message));
                    } else {
                        // Unknown error -> probably lost connection -> try to reconnect
                        this.disconnect();
                        console.info('Websocket lost connection. Reconnecting...');
                        setTimeout(this.connect, 5000);
                    }
                }
            }),
        );
    };

    onMessage = frame => {
        var body = JSON.parse(frame.body);
        const eventType = body.eventType;
        console.info('WEBSOCKET MESSAGE:', body);

        if (this.eventMap[eventType]) {
            this.eventMap[eventType](body);
        } else {
            console.warn("Unknown event type '" + eventType + "'", body);
        }
    };

    onReceipt = frame => {
        let {body, headers} = frame;
        const receiptId = headers['receipt-id'];
        console.info('WEBSOCKET RECEIPT:', frame, '| Remaining requests:', this.pendingRequests);

        let request = receiptId && this.pendingRequests[receiptId];

        if (request) {
            const bodyObj = JSON.parse(body);
            if (bodyObj && !bodyObj.errorMessage) {
                request.onSuccess(bodyObj);
            } else {
                request.onError(bodyObj);
            }
            delete this.pendingRequests[receiptId];
        } else {
            console.warn('Unknown request - id:', receiptId);
        }
    };
}

/**
 * Zpracování eventů.
 *
 * @param values {array} seznam příchozí eventů
 */

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
    ISSUE_LIST_UPDATE: issueListUpdate,
    ISSUE_LIST_CREATE: issueListCreate,
    ISSUE_UPDATE: issueUpdate,
    ISSUE_CREATE: issueCreate,
};

if (!window.ws) {
    window.ws = new websocket(wsUrl, eventMap);
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
    store.dispatch(changeApproveVersion(value.fundId, value.versionId));
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
        store.dispatch(addToastrSuccess(i18n('arr.functions.persistentSort.sortSuccess')));
    } else if (value.state === 'INTERRUPTED') {
        store.dispatch(addToastrDanger(i18n('arr.functions.persistentSort.sortInterrupted')));
    } else if (value.state === 'FAILED') {
        store.dispatch(addToastrDanger(i18n('arr.functions.persistentSort.sortFailed')));
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
        store.dispatch(addToastrSuccess(i18n('arr.functions.computeAndVizualizeEJ.success')));
    } else if (value.state === 'INTERRUPTED') {
        store.dispatch(addToastrDanger(i18n('arr.functions.computeAndVizualizeEJ.interrupted')));
    } else if (value.state === 'FAILED') {
        store.dispatch(addToastrDanger(i18n('arr.functions.computeAndVizualizeEJ.error')));
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

function issueListUpdate({id}) {
    store.dispatch(issuesActions.protocol.invalidate(id));
    store.dispatch(issuesActions.protocols.invalidate());
}

function issueListCreate({id}) {
    store.dispatch(issuesActions.protocol.invalidate(id));
    store.dispatch(issuesActions.protocols.invalidate());
}

function issueUpdate({issueListId, ids}) {
    store.dispatch(issuesActions.list.invalidate(issueListId));
    store.dispatch(issuesActions.detail.invalidate(issueListId));
    ids.forEach(id => {
        store.dispatch(issuesActions.comments.invalidate(id));
    });
}

function issueCreate({issueListId}) {
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
