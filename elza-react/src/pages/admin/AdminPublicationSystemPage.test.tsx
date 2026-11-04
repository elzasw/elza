import { describe, expect, it, vi } from 'vitest';
import { HttpResponse, http } from 'msw';
import userEvent from '@testing-library/user-event';
import { Route } from 'react-router-dom';
import { ConnectionType, PublicationType } from 'elza-api';

import { server } from 'test/mocks/server';
import { renderWithProviders, screen } from 'test/test-utils';
import { AdminPublicationSystemPage } from './AdminPublicationSystemPage';

const SYSTEMS: PublicationType[] = [
    {
        id: 2,
        name: 'Veřejný portál',
        code: 'portal',
        retentionCount: 5,
        active: true,
        allowPermExport: true,
        allowPermPublication: true,
        connectionType: ConnectionType.Production,
    },
    {
        id: 1,
        name: 'Archivní portál',
        code: 'archiv',
        retentionCount: 5,
        active: false,
        allowPermExport: false,
        allowPermPublication: false,
        connectionType: ConnectionType.Test,
    },
];

const PRELOADED_STATE: Record<string, unknown> = {
    userDetail: {
        id: 1,
        username: 'test',
        userPermissions: {},
        permissionsMap: {},
        authTypes: [] as string[],
        fetched: true,
        fetching: false,
    },
};

function useSystemsHandler(systems: PublicationType[]) {
    server.use(
        http.get('/api/v1/publication/types', () => HttpResponse.json(systems)),
        http.get('/api/rule/exportFilters', () => HttpResponse.json([])),
    );
}

function renderPage(route = '/admin/publicationSystem') {
    return renderWithProviders(
        <Route path="/admin/publicationSystem/:id?" component={AdminPublicationSystemPage} />,
        { route, preloadedState: PRELOADED_STATE },
    );
}

describe('AdminPublicationSystemPage', () => {
    it('vypíše systémy podle názvu s kódem, typem připojení a příznakem neaktivního', async () => {
        useSystemsHandler(SYSTEMS);

        renderPage();

        const names = await screen.findAllByText(/portál$/);
        expect(names.map(name => name.textContent)).toEqual(['Archivní portál', 'Veřejný portál']);
        expect(screen.getByText('archiv')).toBeInTheDocument();
        expect(screen.getByText('Produkce')).toBeInTheDocument();
        expect(screen.getByText('Neaktivní')).toBeInTheDocument();
    });

    it('kliknutí na systém otevře jeho detail a Zpět vrátí seznam', async () => {
        useSystemsHandler(SYSTEMS);
        const user = userEvent.setup();

        renderPage();

        await user.click(await screen.findByText('Veřejný portál'));

        expect(await screen.findByText('Veřejný portál')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Uložit' })).toBeInTheDocument();
        expect(screen.queryByText(/je deaktivovaný/)).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Zpět' }));

        expect(await screen.findByRole('button', { name: 'Přidat publikační systém' })).toBeInTheDocument();
    });

    it('nový systém se založí s kódem odvozeným z názvu', async () => {
        useSystemsHandler([]);
        let createdBody: PublicationType | undefined;
        server.use(
            http.post('/api/v1/publication/types', async ({ request }) => {
                createdBody = (await request.json()) as PublicationType;
                return HttpResponse.json({ ...createdBody, id: 3 });
            }),
        );
        const user = userEvent.setup();

        renderPage();

        expect(await screen.findByText('Žádné publikační systémy')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Přidat publikační systém' }));

        expect(await screen.findByText('Nový publikační systém')).toBeInTheDocument();
        await user.type(screen.getByRole('textbox', { name: 'Název' }), 'Nový portál');
        await user.click(screen.getByRole('button', { name: 'Uložit' }));

        await vi.waitFor(() => expect(createdBody).toMatchObject({ name: 'Nový portál', code: 'novy_portal' }));
        expect(await screen.findByRole('button', { name: 'Přidat publikační systém' })).toBeInTheDocument();
    });

    it('detail neaktivního systému otevřený přímo z adresy jde upravit', async () => {
        useSystemsHandler(SYSTEMS);

        renderPage('/admin/publicationSystem/1');

        expect(await screen.findByText('Archivní portál')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Aktivovat' })).toBeInTheDocument();
        expect(screen.getByText('Publikační systém je deaktivovaný, nelze do něj publikovat.')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Název' })).toBeEnabled();
    });
});
