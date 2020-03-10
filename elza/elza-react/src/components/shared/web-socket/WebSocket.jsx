import React from 'react';
import {connect} from 'react-redux';
import {Button} from '../../ui';

import './WebSocket.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import i18n from '../../i18n';

class WebSocket extends AbstractReactComponent {
    render() {
        const {webSocket, login} = this.props;
        const {loading} = this.props.webSocket;

        let content;

        if (loading) {
            content = <div className="dialog">
                <span className="title">{i18n('global.websocket.title.loading')}</span>
                <span className="message">{i18n('global.websocket.message.loading')}</span>
            </div>;
        } else {
            if (webSocket.disconnectedOnError) {
                content = <div className="dialog">
                    <div className="title">{i18n('global.websocket.disconnectedOnError.title')}</div>
                    <br/>
                    <Button onClick={() => {
                        window.ws.connect();
                    }}>{i18n('global.websocket.disconnectedOnError.action.refresh')}</Button>
                </div>;
            } else {
                content = <div className="dialog">
                    <span className="title">{i18n('global.websocket.title')}</span>
                    <span className="message">{i18n('global.websocket.message')}</span>
                </div>;
            }
        }

        const showWebsocketMessage = !webSocket.connected && login.logged;

        return <div className="web-socket">
            {showWebsocketMessage && <div className="disconnect">
                {content}
            </div>}
        </div>;
    }
}

function mapStateToProps(state) {
    const {webSocket, login} = state;
    return {
        webSocket,
        login,
    };
}

export default connect(mapStateToProps)(WebSocket);
