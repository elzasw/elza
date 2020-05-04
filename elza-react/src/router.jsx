/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import {Provider} from 'react-redux';

import Layout from 'pages/Layout.jsx';
import {Route} from 'react-router';
import {BrowserRouter} from 'react-router-dom';

const serverContextPath = window.serverContextPath;

class Root extends React.Component {
    render() {
        return (
            <Provider store={this.props.store} key="provider">
                <BrowserRouter
                    key="router"
                    basename={serverContextPath.startsWith('http') ? '' : serverContextPath}
                >
                    <Route component={Layout} />
                </BrowserRouter>
            </Provider>
        );
    }
}

export default Root;
