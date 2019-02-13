/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import {hot} from 'react-hot-loader'
import { Provider } from 'react-redux'


import Layout from 'pages/Layout.jsx';
import {Route} from "react-router";
import {BrowserRouter} from "react-router-dom";

class Root extends React.Component {

    devTools() {
        const elements = [];
        if (typeof __DEVTOOLS__ !== 'undefined' && __DEVTOOLS__) {
            /*eslint-disable*/
            const DevTools = require('./DevTools').default;
            /*eslint-enable*/
            elements.push(<DevTools key="devtools" />)
        }

        return elements
    }

    render() {
        return <Provider store={this.props.store} key="provider">
            <div style={{height: '100%'}}>
                <BrowserRouter key="router" basename={serverContextPath.startsWith("http") ? "" : serverContextPath}>
                    <Route component={Layout} />
                </BrowserRouter>
                {this.devTools()}
            </div>
        </Provider>
    }
}



export default hot(module)(Root)

