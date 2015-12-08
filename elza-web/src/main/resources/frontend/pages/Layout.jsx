/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { ResizeStore } from 'stores';

//var Ukazky = require('./../components/Ukazky.jsx');

require('./Layout.less');

module.exports = class Layout extends React.Component {
    render1() {
        return <Ukazky/>
    }
    render() {
        return (
            <div className='root-container'>
                {this.props.children}
            </div>
        )
    }
}
