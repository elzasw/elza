/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { Router, Route, IndexRoute } from 'react-router';
import { createHistory, useBasename, useQueries } from 'history';
import { Provider } from 'react-redux'
import { AppStore } from 'stores'

import useRouterHistory from 'react-router/lib/useRouterHistory'

const browserHistory = useRouterHistory(useBasename(createHistory))({
    basename: serverContextPath + ''
});

import {ArrPage, HomePage, RegistryPage, PartyPage, FundPage, AdminLayout, AdminPage, AdminPackagesPage, AdminFulltextPage, Layout} from 'pages';

// Aplikace
exports.start = function() {
    ReactDOM.render((
        <Provider store={AppStore.store}>
            <Router history={browserHistory}>
                {routes}
            </Router>
        </Provider>
    ), document.getElementById('content'));
}

var routes = (
        <Route name="layout" path="/" component={Layout}>
            <IndexRoute component={HomePage} />
            <Route path="fund" component={FundPage} />
            <Route path="arr" component={ArrPage} />
            <Route path="registry" component={RegistryPage} />
            <Route path="party" component={PartyPage} />
            <Route path="admin" component={Layout}>
                <IndexRoute component={AdminPage} />
                <Route path="packages" component={AdminPackagesPage} />
                <Route path="fulltext" component={AdminFulltextPage} />
            </Route>
        </Route>
);



