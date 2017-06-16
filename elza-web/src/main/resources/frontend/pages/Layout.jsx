/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import { AppStore, ResizeStore } from 'stores/index.jsx';
import {AbstractReactComponent, ContextMenu, Toastr, ModalDialog, WebSocket, Login} from 'components/index.jsx';
var AppRouter = require ('./AppRouter')
import {ShortcutManager} from 'react-shortcuts';
import {Shortcuts} from 'react-shortcuts';
import {routerNavigate} from 'actions/router.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import Tetris from "components/game/Tetris.jsx";
import keymap from "keymap.jsx";

require('./Layout.less');

const shortcutManager = new ShortcutManager(keymap)

var _gameRunner = null;

class Layout extends AbstractReactComponent {

    state = {
        showGame: false,
        canStartGame: false,
    };

    componentWillUnmount() {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'home':
                this.dispatch(routerNavigate('/'));
                this.dispatch(setFocus('home', 1, 'list'));
                break;
            case 'arr':
                this.dispatch(routerNavigate('/arr'));
                this.dispatch(setFocus('arr', 1, 'tree'));
                break;
            case 'party':
                this.dispatch(routerNavigate('/party'));
                this.dispatch(setFocus('party', 1, 'tree'));
                break;
            case 'registry':
                this.dispatch(routerNavigate('/registry'));
                this.dispatch(setFocus('registry', 1, 'list'));
                break;
            case 'admin':
                this.dispatch(routerNavigate('/admin'));
                break;
        }
    };

    handleGameStartLeave = () => {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
            _gameRunner = null;
        }
        this.setState({canStartGame: false});
    };

    handleGameStartOver = () => {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
            _gameRunner = null;
        }
        _gameRunner = setTimeout(() => {
            this.setState({canStartGame: true});
        }, 1000);
    };
    componentWillMount(){
        this.dispatch({type:"SHORTCUTS_SAVE",shortcutManager:shortcutManager});
    }
    render() {
        const {canStartGame, showGame} = this.state;

        if (showGame) {
            return <Tetris onClose={() => { this.setState({showGame: false, canStartGame: false}) }} />;
        }

        return <Shortcuts name='Main' handler={this.handleShortcuts} global>
            <div className={versionNumber ? 'root-container with-version' : 'root-container'}>
                <div onClick={() => { canStartGame && this.setState({showGame: true}) }} onMouseEnter={this.handleGameStartOver} onMouseLeave={this.handleGameStartLeave} className={"game-placeholder " + (canStartGame ? "canStart" : "")}>
                    &nbsp;
                </div>
                {this.props.children}
                <div style={{overflow:'hidden'}}>
                    <Toastr.Toastr />
                </div>
                <ContextMenu {...this.props.contextMenu}/>
                <ModalDialog {...this.props.modalDialog}/>
                <WebSocket />
                <Login />
                <AppRouter/>
            </div>
            {typeof versionNumber != "undefined" && <div className="version-container">Verze sestavení aplikace: {versionNumber}</div>}
        </Shortcuts>
    }
}
Layout.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

function mapStateToProps(state) {
    const {contextMenu, modalDialog} = state
    return {
        contextMenu,
        modalDialog
    }
}

export default connect(mapStateToProps)(Layout);
