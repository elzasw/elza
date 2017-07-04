import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';

import './WebSocket.less';
import AbstractReactComponent from "../../AbstractReactComponent";
import i18n from "../../i18n";

class WebSocket extends AbstractReactComponent {

    render() {
        const {webSocket} = this.props;
        const {loading} = this.props.webSocket;

        let content;

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
                    <Button onClick={() => { window.ws.stompConnect() }}>{i18n("global.websocket.disconnectedOnError.action.refresh")}</Button>
                </div>
            } else {
                content = <div className="dialog">
                    <span className="title">{i18n('global.websocket.title')}</span>
                    <span className="message">{i18n('global.websocket.message')}</span>
                </div>
            }
        }

        const dialog = !this.props.webSocket.connected ? <div className="disconnect">{content}</div> : null;

        return <div className="web-socket">{dialog}</div>
    }
}

function mapStateToProps(state) {
    const {webSocket} = state
    return {
        webSocket
    }
}

export default connect(mapStateToProps)(WebSocket);
