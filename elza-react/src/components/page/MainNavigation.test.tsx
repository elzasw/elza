import React from 'react';
import { describe, it, expect } from 'vitest';

import { renderWithProviders, screen } from 'test/test-utils';
import { AuthType } from 'typings/store';
import { MainNavigation } from './MainNavigation';

import enCatalog from '../../../lang/translated/en.json';

/**
 * Hlavní rozcestník je vidět na každé stránce, takže chybějící překlad je zde
 * nejdražší. Test proto kreslí proti *skutečnému* katalogu z `lang/translated/`
 * (tedy zdroji pravdy, který je verzovaný), ne proti ručně psanému seznamu
 * zpráv - jinak by prošel i stav, kdy id v kódu existuje, ale v katalogu chybí,
 * a uživatel by v anglickém UI viděl český text.
 *
 * `public/static/res/locale/` se záměrně nepoužívá: je generovaný a gitignorovaný,
 * takže na čerstvém checkoutu před `locale:compile` neexistuje.
 */
const enMessages: Record<string, string> = Object.fromEntries(
    Object.entries(enCatalog as Record<string, { defaultMessage: string }>).map(
        ([id, entry]) => [id, entry.defaultMessage],
    ),
);

/** Uživatel s ADMIN právem - `hasOne` pak vrací true pro všechny položky. */
const adminState = {
    userDetail: {
        id: 1,
        username: 'test',
        userPermissions: {},
        permissionsMap: { ADMIN: { permission: 'ADMIN' } },
        authTypes: [] as AuthType[],
        fetched: true,
        fetching: false,
    },
};

describe('MainNavigation', () => {
    it('renders the Czech source strings from defaultMessage', () => {
        renderWithProviders(<MainNavigation />, { preloadedState: adminState });

        expect(screen.getByText('Domů')).toBeInTheDocument();
        expect(screen.getByText('Archivní soubory')).toBeInTheDocument();
        expect(screen.getByText('Archivní balíčky')).toBeInTheDocument();
        expect(screen.getByText('Archivní entity')).toBeInTheDocument();
        expect(screen.getByText('Administrace')).toBeInTheDocument();
    });

    it('renders English from the committed en catalog', () => {
        renderWithProviders(<MainNavigation />, {
            preloadedState: adminState,
            locale: 'en',
            messages: enMessages,
        });

        expect(screen.getByText('Home')).toBeInTheDocument();
        expect(screen.getByText('Archival funds')).toBeInTheDocument();
        expect(screen.getByText('Archival packages')).toBeInTheDocument();
        expect(screen.getByText('Archival entities')).toBeInTheDocument();
        expect(screen.getByText('Administration')).toBeInTheDocument();
    });

    it('hides the funds and administration entries without the permissions', () => {
        renderWithProviders(<MainNavigation />, {
            preloadedState: {
                userDetail: { ...adminState.userDetail, permissionsMap: {} },
            },
        });

        expect(screen.getByText('Domů')).toBeInTheDocument();
        expect(screen.getByText('Archivní balíčky')).toBeInTheDocument();
        expect(screen.queryByText('Archivní soubory')).not.toBeInTheDocument();
        expect(screen.queryByText('Administrace')).not.toBeInTheDocument();
    });
});
