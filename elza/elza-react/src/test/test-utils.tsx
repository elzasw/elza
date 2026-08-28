import React, { PropsWithChildren, ReactElement } from 'react';
import { render, RenderOptions, RenderResult } from '@testing-library/react';
import { Provider } from 'react-redux';
import { IntlProvider } from 'react-intl';
import { MemoryRouter } from 'react-router-dom';
import { applyMiddleware, createStore, Store } from 'redux';
import thunk from 'redux-thunk';

// Warm up the module graph BEFORE importing rootReducer. The reducer tree has
// a latent circular dependency (stores/app/fund/fundDetail.jsx →
// stores/app/arr/fundTree.jsx → components/shared → ...) that webpack hides
// but Vitest's ESM loader exposes. Importing AppStore first mirrors the
// production load order: it imports components/Utils.jsx and a side-effect
// action module before reducers.jsx, which is enough to resolve the cycle.
// See the circular-dependency finding in refactoring.md.
import '../stores/AppStore';

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

/**
 * Testy vykreslují s prázdným katalogem a spoléhají na defaultMessage; hlášení
 * o chybějícím překladu proto není chyba. Ostatní chyby react-intl se hlásí dál.
 */
const ignoreMissingTranslation = (error: {code?: string}) => {
    if (error?.code === 'MISSING_TRANSLATION') {
        return;
    }
    console.error(error);
};

export function renderWithProviders(
    ui: ReactElement,
    {
        store,
        preloadedState,
        route = '/',
        locale = 'cs',
        messages = {},
        ...renderOptions
    }: RenderWithProvidersOptions = {},
): RenderResult & { store: TestStore } {
    const resolvedStore = store ?? createTestStore(preloadedState);
    const Wrapper: React.FC<PropsWithChildren> = ({ children }) => (
        <Provider store={resolvedStore}>
            <IntlProvider locale={locale} messages={messages} onError={ignoreMissingTranslation}>
                <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
            </IntlProvider>
        </Provider>
    );

    return { store: resolvedStore, ...render(ui, { wrapper: Wrapper, ...renderOptions }) };
}

export * from '@testing-library/react';
