var React = require('react');

import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {stompConnect} from "websocket"

require('./WebSocket.less');

var WebSocket = class extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    render() {
        const {webSocket} = this.props;

        var loading = webSocket.loading;

        var content;

        if (loading) {
            content = <div className="dialog">
                <span className="title">{i18n('global.websocket.title.loading')}</span>
                <span className="message">{i18n('global.websocket.message.loading')}</span>
            </div>
        } else {
            if (webSocket.disconnectedOnError) {
                content = <div className="dialog">
                    <div className="title">{i18n('global.websocket.disconnectedOnError.title')}</div>
                    <br/>
                    <Button onClick={() => { stompConnect() }}>{i18n("global.websocket.disconnectedOnError.action.refresh")}</Button>
                </div>
            } else {
                content = <div className="dialog">
                    <span className="title">{i18n('global.websocket.title')}</span>
                    <span className="message">{i18n('global.websocket.message')}</span>
                </div>
            }
        }

        var dialog = !webSocket.connected ?
            <div className="disconnect">
                {content}
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