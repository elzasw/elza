/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';

var appState = {
    findingAids: {
        selId: 111,
        items: [
            {
                id: 111,
                name: 'xxx1',
                
            },
            {
                id: 222,
                name: 'xxx2'
            },
        ]
    }
};

module.exports = class Layout extends React.Component {
    render() {
        return (
            <div>
                {this.props.children}
            </div>
        )
    }
}
