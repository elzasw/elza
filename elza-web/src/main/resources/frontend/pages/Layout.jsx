/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';

module.exports = class Layout extends React.Component {
    render() {
        return (
            <div>
                {this.props.children}
            </div>
        )
    }
}
