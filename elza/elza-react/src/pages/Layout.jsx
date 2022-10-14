/**
 * Globální layout stránek - obsahuje komponenty podle přepnuté hlavní oblasti, např. Archivní pomůcky, Rejstříky atp.
 */
import { setFocus } from 'actions/global/focus.jsx';
import { routerNavigate } from 'actions/router.jsx';
import Tetris from 'components/game/Tetris.jsx';
import { AbstractReactComponent, ContextMenu, ModalDialog, Toastr, Utils, WebSocket } from 'components/shared';
import keymap from 'keymap.jsx';
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
    EntityCreatePage,
    FundActionPage,
    FundPage,
    HomePage,
    MapPage,
    RegistryPage
} from 'pages';
import { PropTypes } from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Switch } from 'react-router-dom';
import { Shortcuts } from 'react-shortcuts';
import CrossTabHelper, { CrossTabEventType } from "../components/CrossTabHelper";
import Login from '../components/shared/login/Login';
import {
    ACTIONS,
    DAOS,
    FOCUS_KEYS,
    getFundVersion, 
    GRID,
    MOVEMENTS,
    OUTPUTS, 
    REQUESTS,
    TREE,
    urlFundTree,
    URL_ENTITY,
    URL_ENTITY_CREATE,
    URL_FUND,
    URL_NODE
} from '../constants.tsx';
import AdminBulkActionPage from './admin/AdminBulkActionPage';
import AppRouter from './AppRouter';
import './Layout.scss';
import defaultKeymap from './LayoutKeymap.jsx';
import { MAP_URL } from './map/MapPage';
// import FundOpenPage from './fund_open/FundOpenPage';

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
        const { activeFund } = this.props;

        console.log('#handleShortcuts', '[' + action + ']', this);
        switch (action) {
            case 'home':
                this.props.dispatch(routerNavigate('/'));
                this.props.dispatch(setFocus(FOCUS_KEYS.HOME, 1, 'list'));
                break;
            case 'arr':
                this.props.dispatch(routerNavigate(urlFundTree(activeFund.id, getFundVersion(activeFund))));
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
                            <Route path={`${URL_FUND}/open`} component={ArrPage}/>
                            <Route path={`${URL_FUND}/:id/v/:versionId`}>
                                <Switch>
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${TREE}`} component={ArrPage} />
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${GRID}`} component={ArrDataGridPage} />
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${MOVEMENTS}`} component={ArrMovementsPage} />
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${OUTPUTS}`} component={ArrOutputPage} />
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${ACTIONS}`} component={FundActionPage} />
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${DAOS}`} component={ArrDaoPage} />
                                    <Route path={`${URL_FUND}/:id/v/:versionId/${REQUESTS}`} component={ArrRequestPage} />
                                    <Route component={ArrPage} />
                                </Switch>
                            </Route>
                            <Route path={`${URL_FUND}/:id`}>
                                <Switch>
                                    <Route path={`${URL_FUND}/:id/${TREE}`} component={ArrPage} />
                                    <Route path={`${URL_FUND}/:id/${GRID}`} component={ArrDataGridPage} />
                                    <Route path={`${URL_FUND}/:id/${MOVEMENTS}`} component={ArrMovementsPage} />
                                    <Route path={`${URL_FUND}/:id/${OUTPUTS}`} component={ArrOutputPage} />
                                    <Route path={`${URL_FUND}/:id/${ACTIONS}`} component={FundActionPage} />
                                    <Route path={`${URL_FUND}/:id/${DAOS}`} component={ArrDaoPage} />
                                    <Route path={`${URL_FUND}/:id/${REQUESTS}`} component={ArrRequestPage} />
                                    <Route component={FundPage} />
                                </Switch>
                            </Route>
                            <Route path={`${URL_FUND}/`} component={FundPage}/>
                            <Route path={URL_NODE + "/:nodeId"} component={ArrPage} />
                            <Route path={URL_ENTITY + "/:id"} component={RegistryPage} />
                            <Route path={URL_ENTITY} component={RegistryPage} />
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
    const {contextMenu, modalDialog, login, arrRegion} = state;
    return {
        contextMenu,
        modalDialog,
        login,
        activeFund: arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null,
    };
}

export default connect(mapStateToProps)(Layout);
