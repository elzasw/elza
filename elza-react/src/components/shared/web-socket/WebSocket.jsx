import React from 'react';
import {connect} from 'react-redux';
import {Button} from '../../ui';

import './WebSocket.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import { FormattedMessage, defineMessages } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    loadingTitle: { id: 'global.websocket.title.loading', defaultMessage: 'Načítání aplikace' },
    loadingMessage: {
        id: 'global.websocket.message.loading',
        defaultMessage: 'Probíhá načítání aplikace, chvilku strpení',
    },
    errorTitle: {
        id: 'global.websocket.disconnectedOnError.title',
        defaultMessage: 'Při komunikaci nastala chyba',
    },
    errorRefresh: {
        id: 'global.websocket.disconnectedOnError.action.refresh',
        defaultMessage: 'Aktualizovat aplikaci',
    },
    title: { id: 'global.websocket.title', defaultMessage: 'Odpojení od serveru' },
    message: { id: 'global.websocket.message', defaultMessage: 'Vyčkejte na automatické obnovení spojení' },
});

class WebSocket extends AbstractReactComponent {
    render() {
        const {webSocket, login} = this.props;
        const {loading} = this.props.webSocket;

        let content;

        if (loading) {
            content = (
                <div className="dialog">
                    <span className="title">{<FormattedMessage {...messages.loadingTitle} />}</span>
                    <span className="message">{<FormattedMessage {...messages.loadingMessage} />}</span>
                </div>
            );
        } else {
            if (webSocket.disconnectedOnError) {
                content = (
                    <div className="dialog">
                        <div className="title">{<FormattedMessage {...messages.errorTitle} />}</div>
                        <br />
                        <Button
                            onClick={() => {
                                window.ws.connect();
                            }}
                        >
                            {<FormattedMessage {...messages.errorRefresh} />}
                        </Button>
                    </div>
                );
            } else {
                content = (
                    <div className="dialog">
                        <span className="title">{<FormattedMessage {...messages.title} />}</span>
                        <span className="message">{<FormattedMessage {...messages.message} />}</span>
                    </div>
                );
            }
        }

        const showWebsocketMessage = !webSocket.connected && login.logged;

        return <div className="web-socket">{showWebsocketMessage && <div className="disconnect">{content}</div>}</div>;
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
