import React from 'react';

import {EmailSettingsActions, ApplicationActions} from 'actions';

var SockJS = require('sockjs-client');
var Stomp = require('stompjs');
var socket = new SockJS(serverContextPath + '/config/websock');
var client = Stomp.over(socket);

import {Toastr, i18n} from 'mc';

client.heartbeat.outgoing = 5000;
client.heartbeat.incoming = 0;
client.connect('guest', 'guest',
    function(frame) {
        client.subscribe('/topic/api/changes', function(body, headers) {
            var changes = JSON.parse(body.body);
            changes.forEach(ch => {
                switch (ch.area) {
                    case 'EMAIL_SETTINGS':
                        EmailSettingsActions.changed(ch);
                    break;
                    case 'APPLICATION':
                        ApplicationActions.changed(ch);
                    break;
                }
            });
        });
    },
    function(error) {
        var message = new Array();
        message.push(<p>{i18n('global.server.connection.lost.message')}</p>);
        message.push(<a href="#" onClick={function(){location.reload()}}>{i18n('global.action.document.reload')}</a>);

        Toastr.Actions.danger({
            title: i18n('global.server.connection.lost.title'),
            message: message
        });
    }
);