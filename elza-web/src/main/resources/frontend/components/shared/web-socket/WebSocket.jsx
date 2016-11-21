import React from 'react';

import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components/index.jsx';

import './WebSocket.less';

class WebSocket extends AbstractReactComponent {

    render() {
        const {loading} = this.props.webSocket;

        let content;

        if (loading) {
            content = <div className="dialog">
                        <span className="title">{i18n('global.websocket.title.loading')}</span>
                        <span className="message">{i18n('global.websocket.message.loading')}</span>
                      </div>
        } else {
            content = <div className="dialog">
                        <span className="title">{i18n('global.websocket.title')}</span>
                        <span className="message">{i18n('global.websocket.message')}</span>
                      </div>
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