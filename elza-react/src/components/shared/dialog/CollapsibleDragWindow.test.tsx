import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useContext } from 'react';
import { act, fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { userDetailChange } from 'actions/user/userDetail';
import { USER_DETAIL_CLEAR } from 'actions/constants/ActionTypes';
import { CollapsibleDragWindow, FluentDialogContext, FluentDialogProvider } from './FluentModalDialog';

/**
 * Floating windows: the window reads its size back from the DOM, and while the window layer is hidden
 * (display: none, e.g. after logout) a 0x0 measurement must not overwrite the size it had. A reveal
 * request expands a collapsed window. Windows survive a logout but not another user logging in.
 */

let resizeCallbacks: (() => void)[] = [];

class ControlledResizeObserver {
    constructor(callback: () => void) {
        resizeCallbacks.push(callback);
    }
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
}

function measureAs(element: HTMLElement, width: number, height: number) {
    vi.spyOn(element, 'getBoundingClientRect').mockReturnValue({ width, height } as DOMRect);
    act(() => resizeCallbacks.forEach((callback) => callback()));
}

function renderWindow() {
    renderWithProviders(
        <CollapsibleDragWindow title="Okno" onClose={vi.fn()} initialWidth={500} initialHeight={400}>
            Obsah
        </CollapsibleDragWindow>
    );
    const windowBody = screen.getByText('Obsah').parentElement;
    if (!windowBody) {
        throw new Error('Window body not found');
    }
    return windowBody;
}

describe('CollapsibleDragWindow', () => {
    beforeEach(() => {
        resizeCallbacks = [];
        vi.stubGlobal('ResizeObserver', ControlledResizeObserver);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('převezme velikost změřenou v DOM', () => {
        const windowBody = renderWindow();

        measureAs(windowBody, 420, 350);

        expect(windowBody.style.width).toBe('420px');
    });

    it('skrytá vrstva oken (rozměr 0x0) velikost okna nepřepíše', () => {
        const windowBody = renderWindow();

        measureAs(windowBody, 0, 0);

        expect(windowBody.style.width).toBe('500px');
        expect(windowBody.style.height).toBe('400px');
    });

    it('změna revealRequest sbalené okno rozbalí', () => {
        const renderWithRequest = (revealRequest: number) => (
            <CollapsibleDragWindow title="Okno" onClose={vi.fn()} revealRequest={revealRequest}>
                Obsah
            </CollapsibleDragWindow>
        );
        const { rerender } = renderWithProviders(renderWithRequest(0));
        const content = () => screen.getByText('Obsah');
        const [collapseButton] = screen.getAllByRole('button');

        fireEvent.click(collapseButton);
        expect(content().style.display).toBe('none');

        rerender(renderWithRequest(1));
        expect(content().style.display).toBe('flex');
    });
});

describe('FluentDialogProvider', () => {
    function WindowLauncher() {
        const { showModal } = useContext(FluentDialogContext);
        function openWindow() {
            showModal<undefined, undefined>({ createDialog: () => <div>Rozepsané okno</div> });
        }
        return <button onClick={openWindow}>Otevřít okno</button>;
    }

    function renderWithOpenWindow() {
        const { store } = renderWithProviders(
            <FluentDialogProvider hidden={false}>
                <WindowLauncher />
            </FluentDialogProvider>,
            { preloadedState: { userDetail: { id: 1, username: 'anna', permissionsMap: {}, userPermissions: [] } } }
        );
        fireEvent.click(screen.getByRole('button', { name: 'Otevřít okno' }));
        expect(screen.getByText('Rozepsané okno')).toBeInTheDocument();
        const logIn = (username: string) =>
            act(() => void store.dispatch(userDetailChange({ id: 2, username, userPermissions: [] })));
        const logOut = () => act(() => void store.dispatch({ type: USER_DETAIL_CLEAR }));
        return { logIn, logOut };
    }

    it('okna přežijí odhlášení a nové přihlášení stejného uživatele', () => {
        const { logIn, logOut } = renderWithOpenWindow();

        logOut();
        logIn('anna');

        expect(screen.getByText('Rozepsané okno')).toBeInTheDocument();
    });

    it('přihlášení jiného uživatele okna zavře', () => {
        const { logIn, logOut } = renderWithOpenWindow();

        logOut();
        logIn('petr');

        expect(screen.queryByText('Rozepsané okno')).not.toBeInTheDocument();
    });
});
