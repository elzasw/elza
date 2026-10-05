import { describe, it, expect, vi } from 'vitest';
import { http, HttpResponse } from 'msw';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { login } from 'actions/global/login';
import { Login } from './Login';

/**
 * Login: while the application has no user, the first-run setup replaces the login form;
 * a failed login shows its reason.
 */

vi.mock('actions/global/login', async importOriginal => ({
    ...(await importOriginal<Record<string, unknown>>()),
    // the logged-out state comes from the preloaded store, not from the server
    checkUserLogged: vi.fn(() => () => undefined),
    login: vi.fn(() => () => Promise.resolve()),
}));

const renderLoggedOut = () =>
    renderWithProviders(<Login />, {
        preloadedState: {
            login: { logged: false },
            userDetail: { id: null, username: '', userPermissions: [], permissionsMap: {}, fetched: true, fetching: false },
        },
    });

const loginFailsWith = (data: Record<string, unknown>) =>
    vi.mocked(login).mockImplementationOnce(() => () => Promise.reject({ type: 'unauthorized', data }));

describe('Login', () => {
    it('shows the login form when users exist', async () => {
        renderLoggedOut();

        expect(await screen.findByText('Přihlášení uživatele')).toBeInTheDocument();
        expect(screen.queryByText('Úvodní nastavení')).toBeNull();
    });

    it('shows the first-run setup when no user exists', async () => {
        server.use(http.get('/api/v1/setup', () => HttpResponse.json({ setupRequired: true })));
        renderLoggedOut();

        expect(await screen.findByText('Úvodní nastavení')).toBeInTheDocument();
        expect(screen.queryByText('Přihlášení uživatele')).toBeNull();
    });

    it('falls back to the login form when the setup status is unavailable', async () => {
        server.use(http.get('/api/v1/setup', () => new HttpResponse(null, { status: 401 })));
        renderLoggedOut();

        expect(await screen.findByText('Přihlášení uživatele')).toBeInTheDocument();
    });

    it.each([
        [{ code: 'USER_INACTIVE' }, 'Uživatel je deaktivován. Obraťte se na administrátora.'],
        [{ code: 'BAD_CREDENTIALS' }, 'Neplatné uživatelské jméno nebo heslo.'],
        [{}, 'Neznámá chyba přihlášení'],
    ])('shows the reason of a failed login %j', async (data, message) => {
        loginFailsWith(data);
        renderLoggedOut();
        await screen.findByText('Přihlášení uživatele');

        fireEvent.change(screen.getByLabelText(/Uživatelské jméno/), { target: { value: 'novak' } });
        fireEvent.change(screen.getByLabelText(/^Heslo/), { target: { value: 'heslo' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přihlásit' }));

        expect(await screen.findByText(message)).toBeInTheDocument();
    });
});
