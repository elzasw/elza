/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import { AppStore, ResizeStore } from 'stores/index.jsx';
import {AbstractReactComponent, ContextMenu, Toastr, ModalDialog, WebSocket, Utils} from 'components/shared';
import Login from "../components/shared/login/Login";
import {Route, Switch} from "react-router-dom";
import AppRouter from './AppRouter'
import {ShortcutManager} from 'react-shortcuts';
import {Shortcuts} from 'react-shortcuts';
import {routerNavigate} from 'actions/router.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import Tetris from "components/game/Tetris.jsx";
import {PropTypes} from 'prop-types';
import keymap from "keymap.jsx";
import defaultKeymap from './LayoutKeymap.jsx';
import {
    ArrPage,
    ArrDataGridPage,
    ArrMovementsPage,
    FundActionPage,
    ArrRequestPage,
    ArrDaoPage,
    ArrOutputPage,
    HomePage,
    RegistryPage,
    PartyPage,
    FundPage,
    AdminPage,
    AdminPackagesPage,
    AdminUserPage,
    AdminGroupPage,
    AdminBulkActionPage,
    AdminFundPage,
    AdminExtSystemPage,
    AdminLogsPage,
    AdminRequestsQueuePage,
    NodePage,
    EntityPage,
} from 'pages'

import './Layout.less';
import {modalDialogShow} from "../actions/global/modalDialog";
import i18n from "../components/i18n";
import RegistryUsageForm from "../components/form/RegistryUsageForm";
import PartyUsageForm from "../components/form/PartyUsageForm";
import {FOCUS_KEYS} from "../constants.tsx";


let _gameRunner = null;

class Layout extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap,keymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    state = {
        showGame: false,
        canStartGame: false,
    };

    componentWillUnmount() {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
        }
    }

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'home':
                this.dispatch(routerNavigate('/'));
                this.dispatch(setFocus(FOCUS_KEYS.HOME, 1, 'list'));
                break;
            case 'arr':
                this.dispatch(routerNavigate('/arr'));
                this.dispatch(setFocus(FOCUS_KEYS.ARR, 1, 'tree'));
                break;
            case 'party':
                this.dispatch(routerNavigate('/party'));
                this.dispatch(setFocus(FOCUS_KEYS.PARTY, 1, 'tree'));
                break;
            case 'registry':
                this.dispatch(routerNavigate('/registry'));
                this.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 1, 'list'));
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

    //kvůli circular dependency
    handleRegistryShowUsage = (data) => {
        this.dispatch(
            modalDialogShow(this, i18n('registry.registryUsage'), <RegistryUsageForm detail={data}/>)
        );
    };

    //kvůli circular dependency
    handlePartyShowUsage = (data) => {
        this.dispatch(
            modalDialogShow(this, i18n('party.usage.button'), <PartyUsageForm detail={data}/>)
        );
    };

    render() {
        const {canStartGame, showGame} = this.state;

        if (showGame) {
            return <Tetris onClose={() => { this.setState({showGame: false, canStartGame: false}) }} />;
        }



        return <Shortcuts name='Main' handler={this.handleShortcuts} global stopPropagation={false} className="main-shortcuts">
            <div className={versionNumber ? 'root-container with-version' : 'root-container'}>
                <div onClick={() => { canStartGame && this.setState({showGame: true}) }} onMouseEnter={this.handleGameStartOver} onMouseLeave={this.handleGameStartLeave} className={"game-placeholder " + (canStartGame ? "canStart" : "")}>
                    &nbsp;
                </div>
                {this.props.login.logged && <Switch>
                    <Route path="/fund" component={FundPage} />
                    <Route path="/node/:uuid" component={NodePage} />
                    <Route path="/entity/:uuid" component={EntityPage} />
                    <Route path="/arr">
                        <Switch>
                            <Route path="/arr/dataGrid" component={ArrDataGridPage} />
                            <Route path="/arr/movements" component={ArrMovementsPage} />
                            <Route path="/arr/output" component={ArrOutputPage} />
                            <Route path="/arr/actions" component={FundActionPage} />
                            <Route path="/arr/daos" component={ArrDaoPage} />
                            <Route path="/arr/requests" component={ArrRequestPage} />
                            <Route component={ArrPage} />
                        </Switch>
                    </Route>
                    <Route path="/registry" render={() => <RegistryPage onShowUsage={this.handleRegistryShowUsage}/>} />
                    <Route path="/party"  render={() => <PartyPage onShowUsage={this.handlePartyShowUsage}/>} />
                    <Route path="/admin">
                        <Switch>
                            <Route path="/admin/user" component={AdminUserPage} />
                            <Route path="/admin/group" component={AdminGroupPage} />
                            <Route path="/admin/fund" component={AdminFundPage} />
                            <Route path="/admin/packages" component={AdminPackagesPage} />
                            <Route path="/admin/backgroundProcesses" component={AdminBulkActionPage} />
                            <Route path="/admin/requestsQueue" component={AdminRequestsQueuePage} />
                            <Route path="/admin/extSystem" component={AdminExtSystemPage} />
                            <Route path="/admin/logs" component={AdminLogsPage} />
                            <Route component={AdminPage} />
                        </Switch>
                    </Route>
                    <Route component={HomePage} />
                </Switch>}
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

function mapStateToProps(state) {
    const {contextMenu, modalDialog, login} = state
    return {
        contextMenu,
        modalDialog,
        login
    }
}

export default connect(mapStateToProps)(Layout);
