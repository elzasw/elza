import React from 'react';

import {EmailSettingsActions, ApplicationActions} from 'actions/index.jsx';
import {webSocketConnect, webSocketDisconnect} from 'actions/global/webSocket.jsx';
import {buklActionStateChange} from 'actions/arr/bulkActions.jsx';
import {store} from 'stores/AppStore.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'

import {
    changeConformityInfo,
    changeIndexingFinished,
    changePackage,
    changeFiles,
    changePackets,
    changeNodes,
    changeOutputs,
    changeDeleteLevel,
    changeAddLevel,
    changeApproveVersion,
    changeParty,
    changePartyCreate,
    changePartyDelete,
    changeMoveLevel,
    changeRegistryRecord,
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
} from 'actions/global/change.jsx';

import Stomp from 'stompjs';
import URLParse from "url-parse";

const url = new URLParse(serverContextPath + '/stomp');
const wsUrl = "ws://" + url.host + url.pathname;
console.log("Websocekt URL", wsUrl)
var refresh = false;
var stompClient;

/** Připojení websocketů. */
function stompConnect() {
    stompClient = Stomp.client(wsUrl);
    stompClient = stompClient;
    stompClient.heartbeat.outgoing = 20000;
    stompClient.heartbeat.incoming = 20000;
    console.info("Websocket connecting to " + wsUrl);
    stompClient.connect({}, stompSuccessCallback, stompFundilureCallback);
}
stompConnect();

class ws {
    constructor() {
        this.nextReceiptId = 0;
        this.receiptCallbacks = {}; // mapa id receipt na callback funkci
    }

    send = (url, headers, data, callback) => {
        const useHeaders = headers ? headers : {};
        if (callback) {
            useHeaders.receipt = this.nextReceiptId;
            this.receiptCallbacks[this.nextReceiptId] = callback;
            this.nextReceiptId++;
        }
        stompClient.send(url, headers, data);
    }

    processCallback(body, headers) {

    }
    processErrorCallback(receiptId) {

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
//     client.connect('guest', 'guest', stompSuccessCallback, stompFundilureCallback);
// }
//
/**
 * Callback příchozích dat z websocketů.
 * @param frame {object}
 */
function stompSuccessCallback(frame) {
    console.log("::::stompSuccessCallback", frame);
    store.dispatch(webSocketConnect());
    if (!refresh) {
        refresh = true;
    } else {
        location.reload(true);
    }
    stompClient.subscribe('/topic/api/changes', function({body, headers}) {
        console.info("DDDDDDDDD", body, headers);
        window.ws.processCallback(body, headers);

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
    });
}

/**
 * Callback při ztráně spojení.
 *
 * @param error {string} text chyby
 */
function stompFundilureCallback(error) {
    console.log("::::stompFundilureCallback", error);
    store.dispatch(webSocketDisconnect());
    console.error('WebSocket: ' + error);
    stompClient = null;
    setTimeout(stompConnect, 5000);
    console.info('WebSocket: Obnovení spojení za 5 sekund...');
}


/**
 * Zpracování eventů.
 *
 * @param values {array} seznam příchozí eventů
 */
function processEvents(values) {
    values.forEach(value => {

        switch (value.eventType) {

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

            case 'PARTY_CREATE':
                partyCreate(value);
                break;
            case 'PARTY_UPDATE':
                partyUpdate(value);
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
    store.dispatch(userChange(value.ids))
}
function changeGroup(value) {
    store.dispatch(groupChange(value.ids))
}
function deleteGroup(value) {
    store.dispatch(groupDelete(value.ids[0]))
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
    store.dispatch(changeRegistryRecord(value.ids));
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
    store.dispatch(changePartyCreate(value.ids[0]));
}

function partyDelete(value){
    store.dispatch(changePartyDelete(value.ids[0]));
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

// // připojení websocketů
// stompConnect();
//
