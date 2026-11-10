import { describe, it, expect, afterEach, vi } from 'vitest';
import { useSelector } from 'react-redux';
import { Route } from 'react-router-dom';

import { renderWithProviders, screen, fireEvent, createTestStore, act } from 'test/test-utils';
import type { TestStore } from 'test/test-utils';
import { contextMenuShow } from 'actions/global/contextMenu';
import { ContextMenu } from './ContextMenu';
import type { ContextMenuItem } from './ContextMenu';

/**
 * Global context menu: a Fluent menu at the cursor built from the items in the contextMenu store,
 * closed by a chosen item, a click outside, Escape, a window resize and a document scroll.
 */

interface Position {
    x: number;
    y: number;
}

interface TestState {
    contextMenu: { visible: boolean; component: unknown; menu: ContextMenuItem[] | null; position: Position | null };
}

/** Renders the menu the way Layout does: the whole contextMenu store as props. */
function ContextMenuHost() {
    const contextMenu = useSelector((state: TestState) => state.contextMenu);
    return (
        <>
            <ContextMenu {...contextMenu} />
            <Route render={({ location }) => <div data-testid="location">{location.pathname}</div>} />
        </>
    );
}

function renderMenu(store: TestStore = createTestStore()) {
    return { store, ...renderWithProviders(<ContextMenuHost />, { store }) };
}

function showMenu(store: TestStore, items: ContextMenuItem[], position: Position = { x: 30, y: 40 }) {
    act(() => {
        store.dispatch(contextMenuShow(null, items, position));
    });
}

function isVisible(store: TestStore): boolean {
    return (store.getState() as TestState).contextMenu.visible;
}

afterEach(() => {
    vi.restoreAllMocks();
});

describe('ContextMenu', () => {
    it('skrytá nabídka nic nezobrazí', () => {
        renderMenu();

        expect(screen.queryByRole('menu')).toBeNull();
    });

    it('zobrazí položky nabídky', () => {
        const { store } = renderMenu();

        showMenu(store, [
            { key: 'add', label: 'Přidat uzel', onClick: vi.fn() },
            { key: 'grid', label: 'Otevřít v tabulce', to: '/fund/1/grid' },
        ]);

        expect(screen.getByRole('menu')).toBeTruthy();
        expect(screen.getByRole('menuitem', { name: 'Přidat uzel' })).toBeTruthy();
        expect(screen.getByRole('menuitem', { name: 'Otevřít v tabulce' }).getAttribute('href')).toBe('/fund/1/grid');
    });

    it('bez položek nic nezobrazí', () => {
        const { store } = renderMenu();

        showMenu(store, []);

        expect(screen.queryByRole('menu')).toBeNull();
    });

    it('výběr položky spustí její akci a nabídku zavře', () => {
        const onClick = vi.fn();
        const { store } = renderMenu();
        showMenu(store, [{ key: 'add', label: 'Přidat uzel', onClick }]);

        fireEvent.click(screen.getByRole('menuitem', { name: 'Přidat uzel' }));

        expect(onClick).toHaveBeenCalledTimes(1);
        expect(isVisible(store)).toBe(false);
        expect(screen.queryByRole('menu')).toBeNull();
    });

    it('odkaz přejde na svou cestu v aplikaci', () => {
        const { store } = renderMenu();
        showMenu(store, [{ key: 'grid', label: 'Otevřít v tabulce', to: '/fund/1/grid' }]);

        fireEvent.click(screen.getByRole('menuitem', { name: 'Otevřít v tabulce' }));

        expect(screen.getByTestId('location').textContent).toBe('/fund/1/grid');
        expect(isVisible(store)).toBe(false);
    });

    it('odkaz s ctrl nechá otevření na prohlížeči', () => {
        const { store } = renderMenu();
        showMenu(store, [{ key: 'grid', label: 'Otevřít v tabulce', to: '/fund/1/grid' }]);

        // jsdom cannot navigate, so the browser's own handling is stopped after the menu has seen the click
        let isPreventedByMenu: boolean | undefined;
        const stopBrowserNavigation = (event: Event) => {
            isPreventedByMenu = event.defaultPrevented;
            event.preventDefault();
        };
        window.addEventListener('click', stopBrowserNavigation);
        fireEvent.click(screen.getByRole('menuitem', { name: 'Otevřít v tabulce' }), { ctrlKey: true });
        window.removeEventListener('click', stopBrowserNavigation);

        expect(isPreventedByMenu).toBe(false);
        expect(screen.getByTestId('location').textContent).toBe('/');
    });

    it('kliknutí mimo nabídku a Escape ji zavřou', () => {
        const { store } = renderMenu();

        showMenu(store, [{ key: 'add', label: 'Přidat uzel' }]);
        fireEvent.click(document.body);
        expect(isVisible(store)).toBe(false);

        showMenu(store, [{ key: 'add', label: 'Přidat uzel' }]);
        fireEvent.keyDown(screen.getByRole('menuitem', { name: 'Přidat uzel' }), { key: 'Escape' });
        expect(isVisible(store)).toBe(false);
    });

    it('změna velikosti okna a posun dokumentu nabídku zavřou', () => {
        const { store } = renderMenu();

        showMenu(store, [{ key: 'add', label: 'Přidat uzel' }]);
        fireEvent(window, new Event('resize'));
        expect(isVisible(store)).toBe(false);

        showMenu(store, [{ key: 'add', label: 'Přidat uzel' }]);
        fireEvent.scroll(document);
        expect(isVisible(store)).toBe(false);
    });

    it('skrytá nabídka na události okna nereaguje', () => {
        const store = createTestStore();
        const dispatch = vi.spyOn(store, 'dispatch');
        renderMenu(store);

        fireEvent(window, new Event('resize'));
        fireEvent.scroll(document);
        fireEvent.click(document.body);

        expect(dispatch).not.toHaveBeenCalled();
    });
});
