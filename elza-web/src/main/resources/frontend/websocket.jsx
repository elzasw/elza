import React from 'react';

import {EmailSettingsActions, ApplicationActions} from 'actions';
import {webSocketConnect, webSocketDisconnect} from 'actions/global/webSocket'
import {store} from 'stores/app/AppStore';

var SockJS = require('sockjs-client');
var Stomp = require('stompjs');
var socket = new SockJS(serverContextPath + '/config/websock');

var client = Stomp.over(socket);

function stompConnect() {
    console.log('STOMP: Pokus o připojení...');

    socket = new SockJS(serverContextPath + '/web/websock');
    client = Stomp.over(socket);

    client.heartbeat.outgoing = 5000;
    client.heartbeat.incoming = 0;
    client.connect('guest', 'guest', stompSuccessCallback, stompFailureCallback);
}

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

function processEvents(values) {
    values.forEach(value => {

        switch (value.eventType) {



            default:
                console.warn("Nedefinovaný typ eventu: " + value.eventType);
                break;
        }

    });
}

function processValidations(values) {
    values.forEach(value => {

        switch (value.validationType) {



            default:
                console.warn("Nedefinovaný typ validace: " + value.validationType);
                break;
        }

    });
}

function stompFailureCallback(error) {
    store.dispatch(webSocketDisconnect());
    console.log('STOMP: ' + error);
    setTimeout(stompConnect, 5000);
    console.log('STOMP: Obnovení spojení za 5 sekund...');
}

stompConnect();

