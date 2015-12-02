/**
 * Layout stránek - podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';

module.exports = React.createClass({
    render() {
        return (
            <div>
                111
                {this.props.children}
                222
            </div>
        );
    }
});
