/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { Router, Route, IndexRoute } from 'react-router';
import { createHistory, useBasename, useQueries } from 'history';
import { Provider } from 'react-redux'
import { AppStore } from 'stores/index.jsx'

import useRouterHistory from 'react-router/lib/useRouterHistory'

const browserHistory = useRouterHistory(useBasename(createHistory))({
    basename: serverContextPath + ''
});

import {ArrPage, FundActionPage, ArrOutputPage, HomePage, RegistryPage, PartyPage, FundPage, AdminLayout, AdminPage, AdminPackagesPage, AdminFulltextPage, Layout} from 'pages/index.jsx';

// Aplikace
exports.start = function() {
    ReactDOM.render((
        <Provider store={AppStore.store}>
            <Router history={browserHistory}>
                {routes}
            </Router>
        </Provider>
    ), document.getElementById('content'));
};

var routes = (
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
                <Route path="packages" component={AdminPackagesPage} />
                <Route path="fulltext" component={AdminFulltextPage} />
            </Route>
        </Route>
);



