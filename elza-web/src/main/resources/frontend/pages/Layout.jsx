/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { ResizeStore } from 'stores';
import {Toastr} from 'components';

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
                <div style={{overflow:'hidden'}}>
                    <Toastr.Toastr />
                </div>
                
            </div>
        )
    }
}