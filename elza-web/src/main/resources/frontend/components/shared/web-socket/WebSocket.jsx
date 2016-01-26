var React = require('react');

import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components';

require('./WebSocket.less');

var WebSocket = class extends AbstractReactComponent {

    constructor(props) {
        super(props);
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