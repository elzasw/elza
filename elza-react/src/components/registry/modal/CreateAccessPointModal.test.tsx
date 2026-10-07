import { describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';

import { fireEvent, renderWithProviders, screen, waitFor } from 'test/test-utils';
import { server } from 'test/mocks/server';
import CreateAccessPointModal from './CreateAccessPointModal';

/**
 * New archival entity: the scope is chosen first, and the classes offered are those of the rule set
 * of that scope.
 */

const preloadedState = (scopes: Array<{ id: number; name: string }>) => ({
    refTables: {
        scopesData: { scopes: [{ versionId: -1, scopes }] },
        partTypes: { fetched: true, items: [{ id: 3, code: 'PT_NAME', name: 'Označení' }] },
        rulDataTypes: { fetched: true, items: [] },
        descItemTypes: { fetched: true, items: [] },
    },
    app: { apViewSettings: { fetched: true, data: {} } },
    userDetail: { permissionsMap: { AP_SCOPE_WR_ALL: { all: true } }, userPermissions: [] },
});

const serveClasses = () => {
    const scopeIds: (string | null)[] = [];
    server.use(
        http.get('/api/registry/recordTypes', ({ request }) => {
            scopeIds.push(new URL(request.url).searchParams.get('scopeId'));
            return HttpResponse.json([
                { id: 2, code: 'PERSON_INDIVIDUAL', name: 'Fyzická osoba', addRecord: true, children: [] },
            ]);
        }),
    );
    return scopeIds;
};

describe('CreateAccessPointModal', () => {
    it('asks for the scope first and then offers the classes of its rule set', async () => {
        const scopeIds = serveClasses();
        renderWithProviders(
            <CreateAccessPointModal title="Nová archivní entita" onClose={vi.fn()} onSubmit={vi.fn()} />,
            { preloadedState: preloadedState([{ id: 7, name: 'CAM' }, { id: 8, name: 'ISAAR' }]) },
        );

        expect(await screen.findByText('Nová archivní entita')).toBeInTheDocument();
        const classes = screen.getByRole('combobox', { name: /Podtřída/ });
        expect(classes).toBeDisabled();

        fireEvent.click(screen.getByRole('combobox', { name: /Oblast/ }));
        fireEvent.click(await screen.findByRole('option', { name: 'ISAAR' }));

        await waitFor(() => expect(scopeIds).toEqual(['8']));
        await waitFor(() => expect(screen.getByRole('combobox', { name: /Podtřída/ })).toBeEnabled());
    });

    it('presets the only writable scope', async () => {
        const scopeIds = serveClasses();
        renderWithProviders(
            <CreateAccessPointModal title="Nová archivní entita" onClose={vi.fn()} onSubmit={vi.fn()} />,
            { preloadedState: preloadedState([{ id: 7, name: 'CAM' }]) },
        );

        await waitFor(() => expect(scopeIds).toEqual(['7']));
        expect(screen.queryByRole('combobox', { name: /Oblast/ })).toBeNull();
    });

    it('closes on cancel', async () => {
        serveClasses();
        const onClose = vi.fn();
        renderWithProviders(
            <CreateAccessPointModal title="Nová archivní entita" onClose={onClose} onSubmit={vi.fn()} />,
            { preloadedState: preloadedState([{ id: 7, name: 'CAM' }]) },
        );

        fireEvent.click(await screen.findByRole('button', { name: 'Zrušit' }));
        expect(onClose).toHaveBeenCalled();
    });
});
