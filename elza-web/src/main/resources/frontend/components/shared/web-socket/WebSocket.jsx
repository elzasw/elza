var React = require('react');

import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components/index.jsx';

require('./WebSocket.less');

var WebSocket = class extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    render() {

        var loading = this.props.webSocket.loading;

        var content;

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

        var dialog = !this.props.webSocket.connected ?
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