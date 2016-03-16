import React from 'react';

import {EmailSettingsActions, ApplicationActions} from 'actions';
import {webSocketConnect, webSocketDisconnect} from 'actions/global/webSocket';
import {buklActionStateChange} from 'actions/arr/bulkActions';
import {store} from 'stores/app/AppStore';

import {changeConformityInfo, changeIndexingFinished, changePackage, changePackets,
        changeNodes, changeDeleteLevel, changeAddLevel, changeApproveVersion, changeParty,
    changeMoveLevel, changeRegistryRecord, changeFund, changeFundRecord} from 'actions/global/change';


var SockJS = require('sockjs-client');
var Stomp = require('stompjs');
var socket = new SockJS(serverContextPath + '/config/websock');
var client = Stomp.over(socket);
var refresh = false;
/**
 * Připojení websocketů.
 */
function stompConnect() {
    console.info('WebSocket: Pokus o připojení...');

    socket = new SockJS(serverContextPath + '/web/websock');
    client = Stomp.over(socket);
    client.debug = null;

    client.heartbeat.outgoing = 5000;
    client.heartbeat.incoming = 0;
    client.connect('guest', 'guest', stompSuccessCallback, stompFundilureCallback);
}

/**
 * Callback příchozích dat z websocketů.
 * @param frame {object}
 */
function stompSuccessCallback(frame) {
    store.dispatch(webSocketConnect());
    if (!refresh) {
        refresh = true;
    } else {
        location.reload(true);
    }
    client.subscribe('/topic/api/changes', function(body, headers) {
        var change = JSON.parse(body.body);
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
    store.dispatch(webSocketDisconnect());
    console.error('WebSocket: ' + error);
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

            case 'PARTY_CREATE':
            case 'PARTY_UPDATE':
                partyUpdate(value);
                break;
            case 'NODES_CHANGE':
                nodesChange(value);
                break;

            case 'PACKETS_CHANGE':
                packetsChangeEvent(value);
                break;

            case 'BULK_ACTION_STATE_CHANGE':
                bulkActionStateChange(value);
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

            case 'FINDING_AID_UPDATE':
                fundChange(value);
                break;
            case 'FINDING_AID_RECORD_CHANGE':
                fundRecordChange(value);
                break;
            default:
                console.warn("Nedefinovaný typ eventu: " + value.eventType, value);
                break;
        }

    });
}

function approveVersionChange(value) {
    store.dispatch(changeApproveVersion(value.versionId));
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

function fundRecordChange(value) {
    store.dispatch(changeFundRecord(value.versionId, value.nodeId, value.version));
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
 * Změna obalů.
 */
function packetsChangeEvent(value) {
    store.dispatch(changePackets(value.ids[0]));
}

function nodesChange(value) {
    store.dispatch(changeNodes(value.versionId, value.entityIds));
}

function deleteLevelChange(value) {
    store.dispatch(changeDeleteLevel(value.versionId, value.nodeId, value.parentNodeId));
}

/**
 * Změna hromadných akcí.
 */
function bulkActionStateChange(value) {
    store.dispatch(buklActionStateChange(value));
}

function partyUpdate(value){
    store.dispatch(changeParty(value.ids[0]));
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

// připojení websocketů
stompConnect();

