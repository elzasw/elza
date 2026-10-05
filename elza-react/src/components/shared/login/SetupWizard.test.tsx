import { describe, it, expect, vi, beforeEach } from 'vitest';
import { http, HttpResponse } from 'msw';

import { renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { login } from 'actions/global/login';
import { SetupWizard } from './SetupWizard';

/**
 * First-run setup: creates the first administrator, then logs in.
 */

vi.mock('actions/global/login', async importOriginal => ({
    ...(await importOriginal<Record<string, unknown>>()),
    login: vi.fn(() => () => Promise.resolve()),
}));

const field = (label: RegExp) => screen.getByLabelText(label);

const fill = (values: { username?: string; password?: string; passwordAgain?: string }) => {
    if (values.username != null) fireEvent.change(field(/Uživatelské jméno/), { target: { value: values.username } });
    if (values.password != null) fireEvent.change(field(/^Heslo/), { target: { value: values.password } });
    if (values.passwordAgain != null) fireEvent.change(field(/Opakovat heslo/), { target: { value: values.passwordAgain } });
};

const submit = () => fireEvent.click(screen.getByRole('button', { name: 'Vytvořit administrátora' }));

describe('SetupWizard', () => {
    beforeEach(() => {
        vi.mocked(login).mockClear();
    });

    it('requires all fields and matching passwords', async () => {
        const createAdmin = vi.fn();
        server.use(http.post('/api/v1/setup/admin', () => {
            createAdmin();
            return new HttpResponse(null, { status: 204 });
        }));
        renderWithProviders(<SetupWizard onLoginFailed={vi.fn()} />);

        submit();
        expect(await screen.findAllByText('Pole je povinné')).not.toHaveLength(0);

        fill({ username: 'spravce', password: 'Heslo-1', passwordAgain: 'Heslo-2' });
        submit();
        expect(await screen.findByText('Zadaná hesla nejsou stejná')).toBeInTheDocument();
        expect(createAdmin).not.toHaveBeenCalled();
    });

    it('creates the administrator and logs in', async () => {
        let body: unknown;
        server.use(http.post('/api/v1/setup/admin', async ({ request }) => {
            body = await request.json();
            return new HttpResponse(null, { status: 204 });
        }));
        renderWithProviders(<SetupWizard onLoginFailed={vi.fn()} />);

        fill({ username: ' spravce ', password: 'Heslo-1', passwordAgain: 'Heslo-1' });
        submit();

        await waitFor(() => expect(login).toHaveBeenCalledWith('spravce', 'Heslo-1'));
        expect(body).toEqual({ username: ' spravce ', password: 'Heslo-1' });
    });

    it('shows the server error in the dialog', async () => {
        server.use(http.post('/api/v1/setup/admin', () =>
            HttpResponse.json(
                { type: 'UserCode', code: 'SETUP_NOT_AVAILABLE', message: 'Úvodní nastavení již bylo dokončeno' },
                { status: 400 },
            )));
        renderWithProviders(<SetupWizard onLoginFailed={vi.fn()} />);

        fill({ username: 'spravce', password: 'Heslo-1', passwordAgain: 'Heslo-1' });
        submit();

        expect(await screen.findByText(/Úvodní nastavení již bylo dokončeno/))
            .toBeInTheDocument();
        expect(login).not.toHaveBeenCalled();
    });

    it('reports a failed login after the administrator was created', async () => {
        server.use(http.post('/api/v1/setup/admin', () => new HttpResponse(null, { status: 204 })));
        vi.mocked(login).mockImplementationOnce(() => () => Promise.reject(new Error('login failed')));
        const onLoginFailed = vi.fn();
        renderWithProviders(<SetupWizard onLoginFailed={onLoginFailed} />);

        fill({ username: 'spravce', password: 'Heslo-1', passwordAgain: 'Heslo-1' });
        submit();

        await waitFor(() => expect(onLoginFailed).toHaveBeenCalled());
    });

    it('shows and hides both passwords with one switch', () => {
        renderWithProviders(<SetupWizard onLoginFailed={vi.fn()} />);
        const password = field(/^Heslo/);
        const passwordAgain = field(/Opakovat heslo/);
        expect(password).toHaveAttribute('type', 'password');
        expect(passwordAgain).toHaveAttribute('type', 'password');

        fireEvent.click(screen.getAllByRole('button', { name: 'Zobrazit heslo' })[0]);
        expect(password).toHaveAttribute('type', 'text');
        expect(passwordAgain).toHaveAttribute('type', 'text');

        fireEvent.click(screen.getAllByRole('button', { name: 'Skrýt heslo' })[1]);
        expect(password).toHaveAttribute('type', 'password');
        expect(passwordAgain).toHaveAttribute('type', 'password');
    });

    it('offers the login only when asked to', () => {
        const { unmount } = renderWithProviders(<SetupWizard onLoginFailed={vi.fn()} />);
        expect(screen.queryByRole('button', { name: 'Přihlásit' })).toBeNull();
        unmount();

        const onUseDefaultUser = vi.fn();
        renderWithProviders(<SetupWizard onUseDefaultUser={onUseDefaultUser} onLoginFailed={vi.fn()} />);
        fireEvent.click(screen.getByRole('button', { name: 'Přihlásit' }));
        expect(onUseDefaultUser).toHaveBeenCalled();
    });
});
