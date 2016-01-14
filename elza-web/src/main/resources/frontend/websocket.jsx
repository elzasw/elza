/**
 * Komunikace se serverem přes WebSocket.
 */

import React from 'react';

var SockJS = require('sockjs-client');
var Stomp = require('stompjs');
var socket = new SockJS(serverContextPath + '/web/websock');
var client = Stomp.over(socket);

client.heartbeat.outgoing = 5000;
client.heartbeat.incoming = 0;
client.connect('guest', 'guest',
    function(frame) {
        client.subscribe('/topic/api/changes', function(body, headers) {
            var changes = JSON.parse(body.body);
            changes.forEach(ch => {

                switch (ch.area) {
                    case 'xxx_SETTINGS':
                    break;

                    case 'EVENT':
                        console.log(ch.value);
                    break;
                }
            });
        });
    },
    function(error) {
        console.log(error);
    }
);