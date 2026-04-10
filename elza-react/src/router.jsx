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
import { UserProvider, useTheme } from 'contexts/user';
// import { FluentDialogProvider } from 'components/shared/dialog/FluentModalDialog';

const serverContextPath = window.serverContextPath;

function Root({ store }) {
    const isDark = useTheme();
    const theme = isDark ? webDarkTheme : webLightTheme;

    return (
        <Provider store={store} key="provider">
            <UserProvider>
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
            </UserProvider>
        </Provider>
    );
}

export default Root;
