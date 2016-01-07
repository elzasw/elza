/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import { AppStore, ResizeStore } from 'stores';
import {AbstractReactComponent, ContextMenu, Toastr, ModalDialog} from 'components';

require('./Layout.less');
//var Ukazky = require('./../components/Ukazky.jsx');

var Layout = class Layout extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }


    render() {
        return (
            <div className='root-container'>
                {this.props.children}
                <div style={{overflow:'hidden'}}>
                    <Toastr.Toastr />
                </div>
                <ContextMenu {...this.props.contextMenu}/>
                <ModalDialog {...this.props.modalDialog}/>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {contextMenu, modalDialog} = state
    return {
        contextMenu,
        modalDialog,
    }
}

module.exports = connect(mapStateToProps)(Layout);