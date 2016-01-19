var React = require('react');

import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components';

var SockJS = require('sockjs-client');
var Stomp = require('stompjs');
import {webSocketConnect, webSocketDisconnect} from '../../../actions/global/webSocket'

require('./WebSocket.less');

var WebSocket = class extends AbstractReactComponent {

    constructor(props) {
        super(props);
        
        this.socket = null;
        this.client = null;
        this.bindMethods('stompConnect', 'stompSuccessCallback','stompFailureCallback');
    }

    componentDidMount() {
        this.stompConnect();
    }

    stompConnect() {
        console.log('STOMP: Pokus o připojení...');

        this.socket = new SockJS(serverContextPath + '/web/websock');
        this.client = Stomp.over(this.socket);

        this.client.heartbeat.outgoing = 5000;
        this.client.heartbeat.incoming = 0;
        this.client.connect('guest', 'guest', this.stompSuccessCallback, this.stompFailureCallback);
    }

    stompSuccessCallback(frame) {
        this.dispatch(webSocketConnect());
        this.client.subscribe('/topic/api/changes', function(body, headers) {
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
    }

    stompFailureCallback(error) {
        this.dispatch(webSocketDisconnect());
        console.log('STOMP: ' + error);
        setTimeout(this.stompConnect, 5000);
        console.log('STOMP: Obnovení spojení za 5 sekund...');
    }

    render() {

        var dialog = !this.props.webSocket.connected ?
                     <div className="disconnect">
                         <div className="dialog">
                             <span className="title">{i18n('global.websocket.title')}</span>
                             <span className="message">{i18n('global.websocket.message')}</span>
                         </div>
                     </div>

         : "";

        return (
                <div className="web-socket">
                    {dialog}
                </div>
        );
    }
}

function mapStateToProps(state) {
    const {webSocket} = state
    return {
        webSocket
    }
}

module.exports = connect(mapStateToProps)(WebSocket);