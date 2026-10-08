import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';

import { renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { AdminPackagesAvailable } from './AdminPackagesAvailable';

const isaar = (state: string) => ({
    code: 'ISAAR_CPF',
    name: 'ISAAR(CPF) entity description',
    version: 1,
    description: 'International description of entities',
    dependencies: [{ code: 'CZ_BASE', minVersion: 89 }],
    state,
    fileName: 'package-isaar-cpf.zip',
});

describe('AdminPackagesAvailable', () => {
    it('lists the packages of dpkg that are not loaded and marks one for the next start', async () => {
        let marked = false;
        const markRequests: string[] = [];
        server.use(
            http.get('/api/v1/packages/available', () =>
                HttpResponse.json({
                    items: [isaar(marked ? 'MARKED' : 'NOT_LOADED')],
                    restartRequired: marked,
                    restartAvailable: false,
                }),
            ),
            http.post('/api/v1/packages/available/:code/mark', ({ params }) => {
                markRequests.push(String(params.code));
                marked = true;
                return new HttpResponse(null, { status: 204 });
            }),
        );

        renderWithProviders(<AdminPackagesAvailable />);

        expect(await screen.findByText('ISAAR_CPF')).toBeInTheDocument();
        expect(screen.getByText(/CZ_BASE \(89\)/)).toBeInTheDocument();
        expect(screen.getByText(/Nenačte se/)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Restartovat aplikaci/ })).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: /Načíst při příštím startu/ }));

        await waitFor(() => expect(markRequests).toEqual(['ISAAR_CPF']));
        expect(await screen.findByText(/Označené balíčky se načtou při příštím startu/)).toBeInTheDocument();
        expect(screen.getByText(/Restartujte aplikaci/)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Zrušit označení/ })).toBeInTheDocument();
    });

    it('offers the restart when the server can restart itself', async () => {
        server.use(
            http.get('/api/v1/packages/available', () =>
                HttpResponse.json({ items: [isaar('MARKED')], restartRequired: true, restartAvailable: true }),
            ),
        );

        renderWithProviders(<AdminPackagesAvailable />);

        expect(await screen.findByRole('button', { name: /Restartovat aplikaci/ })).toBeInTheDocument();
        expect(screen.queryByText(/Restartujte aplikaci\./)).not.toBeInTheDocument();
    });

    it('says so when every package of dpkg is loaded', async () => {
        renderWithProviders(<AdminPackagesAvailable />);

        expect(await screen.findByText(/Všechny balíčky z adresáře dpkg jsou načteny/)).toBeInTheDocument();
    });
});
