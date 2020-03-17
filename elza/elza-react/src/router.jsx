/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import {Provider} from 'react-redux';

import Layout from 'pages/Layout.jsx';
import {Route} from 'react-router';
import {BrowserRouter} from 'react-router-dom';

const serverContextPath = window.serverContextPath;
const __DEVTOOLS__ = window.__DEVTOOLS__;

class Root extends React.Component {
    devTools() {
        const elements = [];
        // eslint-disable-line
        if (typeof __DEVTOOLS__ !== 'undefined' && __DEVTOOLS__) {
            /*eslint-disable*/
            const DevTools = require('./DevTools').default;
            /*eslint-enable*/
            elements.push(<DevTools key="devtools" />);
        }

        return elements;
    }

    render() {
        return (
            <Provider store={this.props.store} key="provider">
                <div style={{height: '100%'}}>
                    <BrowserRouter
                        key="router"
                        basename={serverContextPath.startsWith('http') ? '' : serverContextPath}
                    >
                        <Route component={Layout} />
                    </BrowserRouter>
                    {this.devTools()}
                </div>
            </Provider>
        );
    }
}

export default Root;
