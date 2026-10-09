import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ReactNode } from 'react';
import { Route } from 'react-router-dom';

import { renderWithProviders, screen } from 'test/test-utils';
import { DigitalRepositoryType } from 'elza-api';
import { AdminDaQueuePage } from './AdminDaQueuePage';

/**
 * The page lists the digital archives (repositories of type DA) and opens the queue of one.
 */

const api = vi.hoisted(() => ({ externalSystemDigitalRepositories: vi.fn() }));
vi.mock('api', () => ({ Api: { externalSystems: api } }));

vi.mock('../shared/layout/AdminLayout', () => ({
    AdminLayout: ({ centerPanel }: { centerPanel: ReactNode }) => <div>{centerPanel}</div>,
}));
vi.mock('components/admin/da-queue/DaQueueList', () => ({
    DaQueueList: ({ repositoryId, repositorySelector }: { repositoryId: number; repositorySelector: ReactNode }) =>
        <div>{repositorySelector}<span>queue of {repositoryId}</span></div>,
}));

const da = (id: number, name: string) => ({ id, code: `DA${id}`, name, type: DigitalRepositoryType.Da });
const serve = (repositories: unknown[]) => api.externalSystemDigitalRepositories.mockResolvedValue({ data: repositories });

const render = (route: string) => renderWithProviders(
    <Route path={['/admin/da/:daId/requests', '/admin/da']} component={AdminDaQueuePage} />,
    { route },
);

describe('AdminDaQueuePage', () => {
    beforeEach(() => api.externalSystemDigitalRepositories.mockReset());

    it('asks for digital archives only and opens the first one', async () => {
        serve([da(302, 'Amadeus')]);
        render('/admin/da');

        expect(await screen.findByText('queue of 302')).toBeInTheDocument();
        expect(screen.getByText('Amadeus')).toBeInTheDocument();
        expect(api.externalSystemDigitalRepositories).toHaveBeenCalledWith(DigitalRepositoryType.Da);
    });

    it('says so when no digital archive is set up', async () => {
        serve([]);
        render('/admin/da');

        expect(await screen.findByText(/Není nastaven žádný digitální archiv/)).toBeInTheDocument();
    });

    it('offers a selector when there are more digital archives', async () => {
        serve([da(302, 'Amadeus'), da(303, 'Druhý DA')]);
        render('/admin/da/303/requests');

        expect(await screen.findByText('queue of 303')).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: 'Digitální archiv' })).toBeInTheDocument();
    });
});
