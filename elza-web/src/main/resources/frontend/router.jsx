/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { Router, Route, IndexRoute, useBasename, useRouterHistory  } from 'react-router';
import { createHistory } from 'history';
import { Provider } from 'react-redux'
import { AppStore } from 'stores/index.jsx'
import defaultImport from 'stores/defaultImport.jsx'

const browserHistory = useRouterHistory(createHistory)({
    basename: serverContextPath + ''
});

import {ArrPage, FundActionPage, ArrOutputPage, HomePage, RegistryPage, PartyPage, FundPage, AdminLayout, AdminPage,
    AdminPackagesPage, AdminUserPage, AdminGroupPage, AdminFulltextPage, Layout} from 'pages/index.jsx';



function createElements (history) {
    const elements = [
        <Router key="router" history={history}>
            <Route name="layout" path="/" component={Layout}>
                <IndexRoute component={HomePage} />
                <Route path="fund" component={FundPage} />
                <Route path="arr">
                    <IndexRoute component={ArrPage} />
                    <Route path="output" component={ArrOutputPage} />
                    <Route path="actions" component={FundActionPage} />
                </Route>
                <Route path="registry" component={RegistryPage} />
                <Route path="party" component={PartyPage} />
                <Route path="admin">
                    <IndexRoute component={AdminPage} />
                    <Route path="user" component={AdminUserPage} />
                    <Route path="group" component={AdminGroupPage} />
                    <Route path="packages" component={AdminPackagesPage} />
                    <Route path="fulltext" component={AdminFulltextPage} />
                </Route>
            </Route>
        </Router>
    ]

    if (typeof __DEVTOOLS__ !== 'undefined' && __DEVTOOLS__) {
        /*eslint-disable*/
        const DevTools = defaultImport(require('./DevTools'));
        /*eslint-enable*/
        elements.push(<DevTools key="devtools" />)
    }

    return elements
}

import {AppContainer} from 'react-hot-loader'
import Redbox from 'redbox-react'

// Aplikace
exports.start = function() {
    if (__DEV__) {
        ReactDOM.render((
        <AppContainer errorReporter={Redbox}>
            <Provider store={AppStore.store} key="provider">
                <div style={{height: '100%'}}>
                    {createElements(browserHistory)}
                </div>
            </Provider>
        </AppContainer>
        ), document.getElementById('content'));
    } else {
        ReactDOM.render((
            <Provider store={AppStore.store} key="provider">
                <div style={{height: '100%'}}>
                    {createElements(browserHistory)}
                </div>
            </Provider>
        ), document.getElementById('content'));
    }
};




