import React from 'react';

import {EmailSettingsActions, ApplicationActions} from 'actions';
import {webSocketConnect, webSocketDisconnect} from 'actions/global/webSocket';
import {store} from 'stores/app/AppStore';

import {changeConformityInfo, changeIndexingFinished} from 'actions/global/change';


var SockJS = require('sockjs-client');
var Stomp = require('stompjs');
var socket = new SockJS(serverContextPath + '/config/websock');
var client = Stomp.over(socket);

/**
 * Připojení websocketů.
 */
function stompConnect() {
    console.log('STOMP: Pokus o připojení...');

    socket = new SockJS(serverContextPath + '/web/websock');
    client = Stomp.over(socket);

    client.heartbeat.outgoing = 5000;
    client.heartbeat.incoming = 0;
    client.connect('guest', 'guest', stompSuccessCallback, stompFailureCallback);
}

/**
 * Callback příchozích dat z websocketů.
 * @param frame {object}
 */
function stompSuccessCallback(frame) {
    store.dispatch(webSocketConnect());
    client.subscribe('/topic/api/changes', function(body, headers) {
        var change = JSON.parse(body.body);
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
function stompFailureCallback(error) {
    store.dispatch(webSocketDisconnect());
    console.log('STOMP: ' + error);
    setTimeout(stompConnect, 5000);
    console.log('STOMP: Obnovení spojení za 5 sekund...');
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

            default:
                console.warn("Nedefinovaný typ eventu: " + value.eventType);
                break;
        }

    });
}

/**
 * Validace uzlu.
 *
 * @param value {object} informace o provedené validace uzlu
 */
function conformityInfo(value) {
    store.dispatch(changeConformityInfo(value.versionId, value.entityId));
}

/**
 * Indexace dokončena.
 */
function indexingFinished() {
    store.dispatch(changeIndexingFinished());
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

