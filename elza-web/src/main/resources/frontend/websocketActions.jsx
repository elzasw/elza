import React from 'react';

import {EmailSettingsActions, ApplicationActions} from 'actions/index.jsx';
import {webSocketConnect, webSocketDisconnect} from 'actions/global/webSocket.jsx';
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import * as daoActions from 'actions/arr/daoActions';
import {store} from 'stores/index.jsx';
import {addToastrDanger} from 'components/shared/toastr/ToastrActions.jsx'
import {i18n} from 'components/shared'

import {
    changeConformityInfo,
    changeIndexingFinished,
    changePackage,
    changeFiles,
    changePackets,
    changeNodes,
    changeNodeRequests,
    changeOutputs,
    changeDeleteLevel,
    changeAddLevel,
    changeApproveVersion,
    changeParty,
    changePartyCreate,
    changePartyDelete,
    createExtSystem,
    updateExtSystem,
    deleteExtSystem,
    changeMoveLevel,
    changeRegistry,
    changeFund,
    deleteFund,
    changeFundRecord,
    changeInstitution,
    changeVisiblePolicy,
    fundOutputChanges,
    fundOutputChangesDetail,
    changeFundAction,
    fundOutputStateChange,
    fundOutputStateChangeToastr,
    userChange,
    groupChange,
    groupDelete,
    fundInvalidChanges,
    createRequest,
    deleteRequest,
    changeRequestItemQueue,
    createRequestItemQueue,
    nodesDelete
} from 'actions/global/change.jsx';

import Stomp from 'stompjs';
import URLParse from "url-parse";

import {reloadUserDetail} from 'actions/user/userDetail'

const url = new URLParse(serverContextPath + '/stomp');

var wsProtocol = url.protocol === "https:" ? "wss:" : "ws:";

const wsUrl = wsProtocol+ "//" + url.host + url.pathname;
console.log("Websocekt URL", wsUrl)
var refresh = false;
var stompClient;

/** Odpojení od websocketů. */
export function stompDisconnect() {
    if (stompClient != null) {
        stompClient.disconnect();
        stompClient = null;
    }
    console.log('Websocket disconnected');
}

/** Připojení websocketů. */
export function stompConnect() {
    stompClient = Stomp.client(wsUrl);
    stompClient = stompClient;
    stompClient.debug = null
    stompClient.heartbeat.outgoing = 20000;
    stompClient.heartbeat.incoming = 20000;
    stompClient.onreceipt = receiptCallback;
    console.info("Websocket connecting to " + wsUrl);
    stompClient.connect({}, stompSuccessCallback, stompFailureCallback);
}
stompConnect();

class ws {
    constructor() {
        this.nextReceiptId = 0;
        this.receiptSuccessCallbacks = {}; // mapa id receipt na callback funkci
        this.receiptErrorCallbacks = {}; // mapa id receipt na error callback funkci
    }

    static stompDisconnect = stompDisconnect;

    static stompConnect = stompConnect;

    send = (url, headers, data, successCallback, errorCallback) => {
        const useHeaders = headers ? headers : {};
        if (successCallback || errorCallback) {
            useHeaders.receipt = this.nextReceiptId;
            this.receiptSuccessCallbacks[this.nextReceiptId] = successCallback;
            this.receiptErrorCallbacks[this.nextReceiptId] = errorCallback;
            this.nextReceiptId++;
        }
        stompClient.send(url, headers, data);
    }

    processCallback(body, headers) {
        if (!body || !headers) {
            return false;
        }

        // console.log("#####processCallback", "body: ", body, "headers: ", headers)
        const receiptIdStr = headers["receipt-id"];
        if ((typeof receiptIdStr === "string" && receiptIdStr.length > 0) || (typeof receiptIdStr === "number")) {
            const receiptId = typeof receiptIdStr === "number" ? receiptIdStr : parseInt(receiptIdStr);

            const bodyObj = JSON.parse(body);
            console.log("__WESOCKET", bodyObj)
            if (bodyObj && bodyObj.errorMessage) {  // error
                if (this.receiptErrorCallbacks[receiptId]) {
                    this.receiptErrorCallbacks[receiptId](bodyObj);
                }
            } else {    // succes
                if (this.receiptSuccessCallbacks[receiptId]) {
                    this.receiptSuccessCallbacks[receiptId](bodyObj);
                }
            }

            // Smazání callback z pole
            this.receiptSuccessCallbacks[receiptId] && delete this.receiptSuccessCallbacks[receiptId];
            this.receiptErrorCallbacks[receiptId] && delete this.receiptErrorCallbacks[receiptId];

            return true;
        }

        return false;
    }
}
if (!window.ws) {
    window.ws = new ws();
}

//
//
//
//
//
//
//
// var SockJS = require('sockjs-client');
// var Stomp = require('stompjs');
// var socket = new SockJS(serverContextPath + '/config/websock');
// var client = Stomp.over(socket);
// var refresh = false;
// /**
//  * Připojení websocketů.
//  */
// function stompConnect() {
//     console.info('WebSocket: Pokus o připojení...');
//
//     socket = new SockJS(serverContextPath + '/web/websock');
//     client = Stomp.over(socket);
//     client.debug = null;
//
//     client.heartbeat.outgoing = 5000;
//     client.heartbeat.incoming = 0;
//     client.connect('guest', 'guest', stompSuccessCallback, stompFailureCallback);
// }
//

function receiptCallback(frame) {
    // console.log("@@@@@@@@@@@@@@@@@@receiptCallback", frame)
}

/**
 * Callback příchozích dat z websocketů.
 * @param frame {object}
 */
function stompSuccessCallback(frame) {
    // console.log("############################## stompSuccessCallback");
    // console.log("::::stompSuccessCallback", frame);
    store.dispatch(webSocketConnect());
    if (!refresh) {
        refresh = true;
    } else {
        location.reload(true);
    }
    stompClient.subscribe('/topic/api/changes', function({body, headers}) {
        // console.log("############### stompClient.subscribe('/topic/api/changes'", "body: ", body, "headers: ", headers)
        if (window.ws.processCallback(body, headers)) { // zpracováno jako callback
            // již zpracováno a není třeba nic dělat
        } else {    // standardní informace o změnách
            var change = JSON.parse(body);
            console.info("WebSocket", change);
            switch (change.area) {
                case 'EVENT':
                    processEvents(change.value);
                    break;
                case 'VALIDATION':
                    processValidations(change.value);
                    break;
                default:
                    console.warn("Nedefinovaný datový typ ze serveru: " + change.area);
                    break;
            }
        }
    });
}

/**
 * Callback při ztráně spojení.
 *
 * @param error {string} text chyby
 */
function stompFailureCallback(error) {
    console.error("Websocket - failure", error);

    const {body, headers} = error;
    if (error.command === "ERROR" && body && headers) {
        stompDisconnect();

        const bodyObj = JSON.parse(body);
        store.dispatch(addToastrDanger(i18n('global.error.ws'), bodyObj.message));
        store.dispatch(webSocketDisconnect(true, bodyObj.message));
    } else {
        store.dispatch(webSocketDisconnect(false));
        setTimeout(stompConnect, 5000);
        console.info('WebSocket: Obnovení spojení za 5 sekund...');
    }
}


/**
 * Zpracování eventů.
 *
 * @param values {array} seznam příchozí eventů
 */
function processEvents(values) {
    values.forEach(value => {

        switch (value.eventType) {

            case 'DAO_LINK_CREATE':
            case 'DAO_LINK_DELETE':
                daoLink(value);
                break;
            case 'REQUEST_CHANGE':
            case 'REQUEST_DAO_CHANGE':
                arrRequest(value);
                break;
            case 'REQUEST_CREATE':
            case 'REQUEST_DAO_CREATE':
                arrRequest(value);
                break;
            case 'CONFORMITY_INFO':
                conformityInfo(value);
                break;

            case 'INDEXING_FINISHED':
                indexingFinished();
                break;

            case 'PACKAGE':
                packageEvent();
                break;

            case 'INSTITUTION_CHANGE':
                institutionChange();
                break;

            case 'PARTY_DELETE':
                partyDelete(value);
                break;

            case 'PARTIES_CREATE':
                partyCreate(value);
                break;
            case 'PARTY_UPDATE':
                partyUpdate(value);
                break;

            case 'EXTERNAL_SYSTEM_CREATE':
                extSystemCreate(value);
                break;
            case 'EXTERNAL_SYSTEM_UPDATE':
                extSystemUpdate(value);
                break;
            case 'EXTERNAL_SYSTEM_DELETE':
                extSystemDelete(value);
                break;

            case 'NODES_CHANGE':
                nodesChange(value);
                break;
            case 'OUTPUT_ITEM_CHANGE':
                outputItemChange(value);
                break;
            case 'PACKETS_CHANGE':
                packetsChangeEvent(value);
                break;

            case 'FILES_CHANGE':
                filesChangeEvent(value);
                break;

            case 'BULK_ACTION_STATE_CHANGE':
                fundActionActionChange(value);
                break;

            case 'DELETE_LEVEL':
                deleteLevelChange(value);
                break;

            case 'ADD_LEVEL_AFTER':
                addLevelAfterChange(value);
                break;

            case 'ADD_LEVEL_BEFORE':
                addLevelBeforeChange(value);
                break;

            case 'ADD_LEVEL_UNDER':
                addLevelUnderChange(value);
                break;

            case 'APPROVE_VERSION':
                approveVersionChange(value);
                break;

            case 'MOVE_LEVEL_AFTER':
                moveLevelAfterChange(value);
                break;

            case 'MOVE_LEVEL_BEFORE':
                moveLevelBeforeChange(value);
                break;

            case 'MOVE_LEVEL_UNDER':
                moveLevelUnderChange(value);
                break;

            case 'RECORD_UPDATE':
                registryChange(value);
                break;

            case 'FUND_UPDATE':
            case 'FUND_CREATE':
                fundChange(value);
                break;
            case 'FUND_RECORD_CHANGE':
                fundRecordChange(value);
                break;

            case 'VISIBLE_POLICY_CHANGE':
                visiblePolicyChange(value);
                break;

            case 'FUND_DELETE':
                fundDelete(value);
                break;

            case 'OUTPUT_STATE_CHANGE':
                outputStateChange(value);
                break;

            case 'OUTPUT_CHANGES':
                outputChanges(value);
                break;

            case 'FUND_INVALID':
                fundInvalid(value);
                break;

            case 'OUTPUT_CHANGES_DETAIL':
                outputChangesDetail(value);
                break;
            case 'USER_CREATE':
            case 'USER_CHANGE':
                changeUser(value);
                break;
            case 'GROUP_CREATE':
            case 'GROUP_CHANGE':
                changeGroup(value);
                break;
            case 'GROUP_DELETE':
                deleteGroup(value);
                break;

            case 'REQUEST_CREATE':
                requestCreate(value);
                break;

            case 'REQUEST_CHANGE':
                requestChange(value);
                break;

            case 'REQUEST_DELETE':
                requestDelete(value);
                break;

            case 'REQUEST_ITEM_QUEUE_CREATE':
            case 'REQUEST_ITEM_QUEUE_DELETE':
                createRequestItemQueueChange(value);
                break;

            case 'REQUEST_ITEM_QUEUE_CHANGE':
                changeRequestItemQueueChange(value);
                break;

            case 'DELETE_NODES':
                deleteNodes(value);
                break;

            default:
                console.warn("Nedefinovaný typ eventu: " + value.eventType, value);
                break;
        }

    });
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
    store.dispatch(groupChange(value.ids))
}
function deleteGroup(value) {
    store.dispatch(groupDelete(value.ids[0]))
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
    store.dispatch(nodesDelete(value.versionId, value.entityIds))
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

/**
 * Změna obalů.
 */
function packetsChangeEvent(value) {
    store.dispatch(changePackets(value.ids[0]));
}

function filesChangeEvent(value) {
    store.dispatch(changeFiles(value.versionId, value.entityId));
}

function nodesChange(value) {
    store.dispatch(changeNodes(value.versionId, value.entityIds));
}
function outputItemChange(value) {
    store.dispatch(changeOutputs(value.versionId, [value.outputDefinitionId]));
}

function deleteLevelChange(value) {
    store.dispatch(changeDeleteLevel(value.versionId, value.nodeId, value.parentNodeId));
}

/**
 * Změna hromadných akcí.
 */
function fundActionActionChange(value) {
    store.dispatch(changeFundAction(value.versionId, value.entityId));
}

function partyUpdate(value){
    store.dispatch(changeParty(value.ids[0]));
}

function partyCreate(value){
    store.dispatch(changePartyCreate(value.ids));
}

function partyDelete(value){
    store.dispatch(changePartyDelete(value.ids[0]));
}

/**
 * Externí systémy
 */

function extSystemCreate(value){
    store.dispatch(createExtSystem(value.ids[0]));
}

function extSystemUpdate(value){
    store.dispatch(updateExtSystem(value.ids[0]));
}

function extSystemDelete(value){
    store.dispatch(deleteExtSystem(value.ids[0]));
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
                console.warn("Nedefinovaný typ validace: " + value.validationType);
                break;
        }

    });
}
