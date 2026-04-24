import React, { PropsWithChildren, ReactElement } from 'react';
import { render, RenderOptions, RenderResult } from '@testing-library/react';
import { Provider } from 'react-redux';
import { IntlProvider } from 'react-intl';
import { MemoryRouter } from 'react-router-dom';
import { applyMiddleware, createStore, Store } from 'redux';
import thunk from 'redux-thunk';

import rootReducer from '../stores/reducers';

export type TestStore = Store;

export function createTestStore(preloadedState?: Record<string, unknown>): TestStore {
    return createStore(
        rootReducer as never,
        preloadedState as never,
        applyMiddleware(thunk),
    );
}

export type RenderWithProvidersOptions = Omit<RenderOptions, 'wrapper'> & {
    store?: TestStore;
    preloadedState?: Record<string, unknown>;
    route?: string;
    locale?: string;
    messages?: Record<string, string>;
};

export function renderWithProviders(
    ui: ReactElement,
    {
        store = createTestStore(),
        route = '/',
        locale = 'cs',
        messages = {},
        ...renderOptions
    }: RenderWithProvidersOptions = {},
): RenderResult & { store: TestStore } {
    const Wrapper: React.FC<PropsWithChildren> = ({ children }) => (
        <Provider store={store}>
            <IntlProvider locale={locale} messages={messages}>
                <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
            </IntlProvider>
        </Provider>
    );

    return { store, ...render(ui, { wrapper: Wrapper, ...renderOptions }) };
}

export * from '@testing-library/react';
