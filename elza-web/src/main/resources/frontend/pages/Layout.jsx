/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import { AppStore, ResizeStore } from 'stores';
import {AbstractReactComponent, ContextMenu, Toastr, ModalDialog, WebSocket} from 'components';
var AppRouter = require ('./AppRouter')
var ShortcutsManager = require('react-shortcuts')
var Shortcuts = require('react-shortcuts/component')
var keyModifier = Utils.getKeyModifier()
import {Utils} from 'components'
import {routerNavigate} from 'actions/router'
import {setFocus} from 'actions/global/focus'

require('./Layout.less');

var keymap = {
    Main: {
        home: 'alt+1',
        arr: 'alt+2',
        registry: 'alt+3',
        party: 'alt+4',
        admin: 'alt+5',
    },
    Tree: {}
}
var shortcutManager = new ShortcutsManager(keymap)

var Layout = class Layout extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleShortcuts');
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'home':
                this.dispatch(routerNavigate('/'))
                this.dispatch(setFocus('home', 1, 'list'))
                break
            case 'arr':
                this.dispatch(routerNavigate('/arr'))
                this.dispatch(setFocus('arr', 1, 'tree'))
                break
            case 'party':
                this.dispatch(routerNavigate('/party'))
                break
            case 'registry':
                this.dispatch(routerNavigate('/registry'))
                this.dispatch(setFocus('registry', 1, 'list'))
                break
            case 'admin':
                this.dispatch(routerNavigate('/admin'))
                break
        }
    }

    render() {
        return (
            <Shortcuts name='Main' handler={this.handleShortcuts}>
                <div className='root-container'>
                    {this.props.children}
                    <div style={{overflow:'hidden'}}>
                        <Toastr.Toastr />
                    </div>
                    <ContextMenu {...this.props.contextMenu}/>
                    <ModalDialog {...this.props.modalDialog}/>
                    <WebSocket />
                    <AppRouter/>
                </div>
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {contextMenu, modalDialog} = state
    return {
        contextMenu,
        modalDialog
    }
}

Layout.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(Layout);