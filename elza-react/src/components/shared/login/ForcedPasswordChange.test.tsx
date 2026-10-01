import { describe, it, expect, vi } from 'vitest';

import { renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
import { ForcedPasswordChange } from './ForcedPasswordChange';

/**
 * Forced password change: shown only to a logged user whose password must be changed,
 * cannot be cancelled, and validates the new password against the policy.
 */

vi.mock('actions/admin/user.jsx', async importOriginal => ({
    ...(await importOriginal<Record<string, unknown>>()),
    userPasswordChange: vi.fn(() => () => Promise.resolve()),
}));

const renderDialog = (needChangePassword: boolean, logged = true) =>
    renderWithProviders(<ForcedPasswordChange />, {
        preloadedState: {
            login: { logged },
            userDetail: {
                id: 10,
                username: 'novak',
                userPermissions: [],
                permissionsMap: {},
                needChangePassword,
                passwordPolicy: { minLength: 8 },
                fetched: true,
                fetching: false,
            },
        },
    });

const passwordInput = (name: string) => document.querySelector<HTMLInputElement>(`input[name="${name}"]`)!;

describe('ForcedPasswordChange', () => {
    it('is shown when the password must be changed', () => {
        renderDialog(true);

        expect(screen.getByText('Změna hesla')).toBeInTheDocument();
        expect(screen.getByText(/Platnost hesla vypršela/)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Zrušit' })).toBeNull();
    });

    it('is hidden when no change is needed', () => {
        renderDialog(false);

        expect(screen.queryByText('Změna hesla')).toBeNull();
    });

    it('is hidden before login', () => {
        renderDialog(true, false);

        expect(screen.queryByText('Změna hesla')).toBeNull();
    });

    it('rejects a password shorter than the policy requires', async () => {
        renderDialog(true);

        fireEvent.change(passwordInput('oldPassword'), { target: { value: 'stare' } });
        fireEvent.change(passwordInput('password'), { target: { value: 'kratke' } });
        fireEvent.change(passwordInput('passwordAgain'), { target: { value: 'kratke' } });
        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));

        await waitFor(() => expect(screen.getByText(/Heslo musí mít alespoň.*\b8\b/)).toBeInTheDocument());
    });
});
