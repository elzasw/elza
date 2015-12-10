/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { Router, Route, IndexRoute } from 'react-router';
import { createHistory, useBasename, useQueries } from 'history';
import { Provider } from 'react-redux'
import { AppStore } from 'stores'

const history = useBasename(createHistory)({
    basename: serverContextPath + ''
})

import {FaPage, RecordPage, PartyPage, Layout} from 'pages';

var routes = (
    <Route name="layout" path="/" component={Layout}>
        <IndexRoute component={FaPage} />
        <Route path="record" component={RecordPage} />
        <Route path="party" component={PartyPage} />
    </Route>
);

// Aplikace
exports.start = function() {
    ReactDOM.render((
        <Provider store={AppStore.store}>
            <Router history={history}>
                {routes}
            </Router>
        </Provider>
    ), document.getElementById('content'));
}



