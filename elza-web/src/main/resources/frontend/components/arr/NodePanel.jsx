/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';

require ('./NodePanel.less');

var NodePanel = class NodePanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className='node-panel-container'>
                <div className='actions'>NODE [{this.props.node.id}] actions</div>
                <div className='parents'>parents<br/>parents<br/>parents<br/>parents<br/></div>
                <div className='content'>content</div>
                <div className='children'>children</div>
            </div>
        );
    }
}

module.exports = NodePanel;