/**
 * Router - mapování URL na VIEW.
 */

import React from 'react';
import { Provider } from 'react-redux';

import Layout from 'pages/Layout.jsx';
import { Route } from 'react-router';
import { BrowserRouter } from 'react-router-dom';
import { FluentProvider, webDarkTheme, webLightTheme } from '@fluentui/react-components';
import { LangProvider } from 'components/shared/lang/LangProvider';

const serverContextPath = window.serverContextPath;

class Root extends React.Component {
    render() {
        const body = document.getElementsByTagName('body')[0];
        const theme = body.className.indexOf('dark') >= 0 ? webDarkTheme : webLightTheme;
        return (
            <Provider store={this.props.store} key="provider">
                <BrowserRouter
                    key="router"
                    basename={serverContextPath.startsWith('http') ? '' : serverContextPath}
                >
                    <LangProvider>
                        <FluentProvider style={{ flex: 1, height: "100%" }} theme={{ ...theme, colorNeutralBackground1: "var(--shade-0)" }}>
                            <Route component={Layout} />
                        </FluentProvider>
                    </LangProvider>
                </BrowserRouter>
            </Provider>
        );
    }
}

export default Root;
