/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import { Router, Route, IndexRoute, useBasename, useRouterHistory  } from 'react-router';
import { createHistory } from 'history';
import { Provider } from 'react-redux'
import defaultImport from 'stores/defaultImport.jsx'

const browserHistory = useRouterHistory(createHistory)({
    basename: serverContextPath + ''
});

import {
    ArrPage,
    ArrDataGridPage,
    ArrMovementsPage,
    FundActionPage,
    ArrRequestPage,
    ArrOutputPage,
    HomePage,
    RegistryPage,
    PartyPage,
    FundPage,
    AdminPage,
    AdminPackagesPage,
    AdminUserPage,
    AdminGroupPage,
    AdminFulltextPage,
    AdminRequestsQueuePage,
    Layout
} from 'pages/index.jsx';

const routes = <Route name="layout" path="/" component={Layout}>
    <IndexRoute component={HomePage} />
    <Route path="fund" component={FundPage} />
    <Route path="arr">
        <IndexRoute component={ArrPage} />
        <Route path="dataGrid" component={ArrDataGridPage} />
        <Route path="movements" component={ArrMovementsPage} />
        <Route path="output" component={ArrOutputPage} />
        <Route path="actions" component={FundActionPage} />
        <Route path="requests" component={ArrRequestPage} />
    </Route>
    <Route path="registry" component={RegistryPage} />
    <Route path="party" component={PartyPage} />
    <Route path="admin">
        <IndexRoute component={AdminPage} />
        <Route path="user" component={AdminUserPage} />
        <Route path="group" component={AdminGroupPage} />
        <Route path="packages" component={AdminPackagesPage} />
        <Route path="fulltext" component={AdminFulltextPage} />
        <Route path="requestsQueue" component={AdminRequestsQueuePage} />
    </Route>
</Route>;

export default class Root extends React.Component {

    devTools() {
        const elements = [];
        if (typeof __DEVTOOLS__ !== 'undefined' && __DEVTOOLS__) {
            /*eslint-disable*/
            const DevTools = defaultImport(require('./DevTools'));
            /*eslint-enable*/
            elements.push(<DevTools key="devtools" />)
        }

        return elements
    }

    render() {
        return <Provider store={this.props.store} key="provider">
            <div style={{height: '100%'}}>
                <Router key="router" history={browserHistory}>{routes}</Router>
                {this.devTools()}
            </div>
        </Provider>
    }
}




