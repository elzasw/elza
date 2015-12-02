/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import ReactDOM from 'react-dom';

import { Router, Route, IndexRoute } from 'react-router';
import { createHistory, useBasename, useQueries } from 'history';

const history = useBasename(createHistory)({
    basename: serverContextPath + ''
})

import {FindingAidPage, RecordPage, PartyPage, Layout} from 'pages';

var routes = (
    <Route name="layout" path="/" component={Layout}>
        <IndexRoute component={FindingAidPage} />
        <Route path="record" component={RecordPage} />
        <Route path="party" component={PartyPage} />
    </Route>
);

// Aplikace
exports.start = function() {
    ReactDOM.render((
        <Router history={history}>
            {routes}
        </Router>
    ), document.getElementById('content'));
}



