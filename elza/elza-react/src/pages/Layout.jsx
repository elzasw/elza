/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, ContextMenu, ModalDialog, Toastr, Utils, WebSocket} from 'components/shared';
import Login from '../components/shared/login/Login';
import {Route, Switch} from 'react-router-dom';
import AppRouter from './AppRouter';
import {Shortcuts} from 'react-shortcuts';
import {routerNavigate} from 'actions/router.jsx';
import {setFocus} from 'actions/global/focus.jsx';
import Tetris from 'components/game/Tetris.jsx';
import {PropTypes} from 'prop-types';
import keymap from 'keymap.jsx';
import defaultKeymap from './LayoutKeymap.jsx';
import {
    AdminExtSystemPage,
    AdminFundPage,
    AdminGroupPage,
    AdminLogsPage,
    AdminPackagesPage,
    AdminPage,
    AdminRequestsQueuePage,
    AdminUserPage,
    ArrDaoPage,
    ArrDataGridPage,
    ArrMovementsPage,
    ArrOutputPage,
    ArrPage,
    ArrRequestPage,
    FundActionPage,
    FundPage,
    HomePage,
    NodePage,
    RegistryPage,
    EntityCreatePage,
    EntityPage,
    MapPage,
} from 'pages';
import './Layout.scss';
import {
    FOCUS_KEYS, REG_NUMBER, REG_UUID,
    URL_ENTITY,
    URL_ENTITY_CREATE,
    URL_FUND, URL_FUND_ACTIONS_PATH, URL_FUND_DAOS_PATH,
    URL_FUND_GRID_PATH,
    URL_FUND_MOVEMENTS_PATH, URL_FUND_OUTPUTS_PATH, URL_FUND_REQUESTS_PATH, URL_FUND_TREE,
    URL_NODE
} from '../constants.tsx';
import AdminBulkActionPage from './admin/AdminBulkActionPage';
import CrossTabHelper, {CrossTabEventType} from "../components/CrossTabHelper";
import {MAP_URL} from './map/MapPage';

let _gameRunner = null;

class Layout extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    child = null;
    parent = null;

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap, keymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    state = {
        showGame: false,
        canStartGame: false,
        polygon: undefined,
        selectedLayer: undefined,
    };

    componentDidMount() {
        CrossTabHelper.init(this);
        window.thisLayout = this;
    }

    componentWillUnmount() {
        if (_gameRunner) {
            clearTimeout(_gameRunner);
        }
        CrossTabHelper.onUnmount(this);
        delete window.thisLayout
    }

    processCrossTabEvent = (event) => {
        if (event.type === CrossTabEventType.SHOW_IN_MAP) {
            this.setState({polygon: event.data});
        }
    };

    handleShortcuts = action => {
        console.log('#handleShortcuts', '[' + action + ']', this);
        switch (action) {
            case 'home':
                this.props.dispatch(routerNavigate('/'));
                this.props.dispatch(setFocus(FOCUS_KEYS.HOME, 1, 'list'));
                break;
            case 'arr':
                this.props.dispatch(routerNavigate(URL_FUND_TREE));
                this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 1, 'tree'));
                break;
            case 'registry':
                this.props.dispatch(routerNavigate(URL_ENTITY));
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 1, 'list'));
                break;
            case 'admin':
                this.props.dispatch(routerNavigate('/admin'));
                break;
            default:
                return null;
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

    handleChangeSelectedLayer = (selectedLayer) => {
        this.setState({selectedLayer})
    }

    render() {
        const {canStartGame, polygon, showGame, selectedLayer} = this.state;

        if (showGame) {
            return (
                <Tetris
                    onClose={() => {
                        this.setState({showGame: false, canStartGame: false});
                    }}
                />
            );
        }

        return (
            <Shortcuts
                name="Main"
                handler={this.handleShortcuts}
                global
                stopPropagation={false}
                className="main-shortcuts"
            >
                <div className={window.versionNumber ? 'root-container with-version' : 'root-container'}>
                    <div
                        onClick={() => {
                            canStartGame && this.setState({showGame: true});
                        }}
                        onMouseEnter={this.handleGameStartOver}
                        onMouseLeave={this.handleGameStartLeave}
                        className={'game-placeholder ' + (canStartGame ? 'canStart' : '')}
                    >
                        &nbsp;
                    </div>
                    {this.props.login.logged && (
                        <Switch>
                            <Route path={URL_FUND_TREE} component={ArrPage} />
                            <Route path={URL_FUND + "/:id(" + REG_NUMBER + ")?"}>
                                <Switch>
                                    <Route path={URL_FUND_GRID_PATH} component={ArrDataGridPage} />
                                    <Route path={URL_FUND_MOVEMENTS_PATH} component={ArrMovementsPage} />
                                    <Route path={URL_FUND_OUTPUTS_PATH} component={ArrOutputPage} />
                                    <Route path={URL_FUND_ACTIONS_PATH} component={FundActionPage} />
                                    <Route path={URL_FUND_DAOS_PATH} component={ArrDaoPage} />
                                    <Route path={URL_FUND_REQUESTS_PATH} component={ArrRequestPage} />
                                    <Route component={FundPage} />
                                </Switch>
                            </Route>
                            <Route path={URL_NODE + "/:uuid(" + REG_UUID + ")"} component={NodePage} />
                            <Route path={URL_NODE + "/:nodeId(" + REG_NUMBER + ")"} component={ArrPage} />
                            <Route path={URL_ENTITY + "/:uuid(" + REG_UUID + ")"} component={EntityPage} />
                            <Route path={URL_ENTITY + "/:id(" + REG_NUMBER + ")?"} component={RegistryPage} />
                            <Route path={URL_ENTITY_CREATE} component={EntityCreatePage} />

                            <Route path={MAP_URL} component={(props) => <MapPage handleChangeSelectedLayer={this.handleChangeSelectedLayer} polygon={polygon} selectedLayer={selectedLayer} {...props} />} />
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
                        </Switch>
                    )}
                    <div style={{overflow: 'hidden'}}>
                        <Toastr.Toastr />
                    </div>
                    <ContextMenu {...this.props.contextMenu} />
                    <ModalDialog {...this.props.modalDialog} />
                    <WebSocket />
                    <Login />
                    <AppRouter />
                </div>
                {typeof window.versionNumber != 'undefined' && (
                    <div className="version-container">Verze sestavení aplikace: {window.versionNumber}</div>
                )}
            </Shortcuts>
        );
    }
}

function mapStateToProps(state) {
    const {contextMenu, modalDialog, login} = state;
    return {
        contextMenu,
        modalDialog,
        login,
    };
}

export default connect(mapStateToProps)(Layout);
