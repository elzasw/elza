import React from 'react';
import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { server } from 'test/mocks/server';
import MultiFundActionDialog from './MultiFundActionDialog';

/**
 * Drives the multi-fund bulk action wizard against MSW-mocked
 * `Api.funds.bulkAction*` endpoints (served by the client under /api/v1).
 */
describe('MultiFundActionDialog', () => {
    it('groups, lets the user pick an action and queues it for the whole group', async () => {
        let queueBody: any = null;

        server.use(
            http.post('/api/v1/action/funds/group', () =>
                HttpResponse.json({
                    groups: [
                        {
                            ruleSetId: 1,
                            ruleSetCode: 'ZP2015',
                            ruleSetName: 'Pravidla ZP2015',
                            fundCount: 3,
                            fundVersionIds: [10, 20, 30],
                            actions: [{ code: 'ContentMigration', name: 'Migrace obsahu', fastAction: false }],
                        },
                    ],
                    skipped: [],
                }),
            ),
            http.post('/api/v1/action/queue-multi', async ({ request }) => {
                queueBody = await request.json();
                return HttpResponse.json({ fundsChangeId: 99, queuedCount: 3, skipped: [] });
            }),
        );

        renderWithProviders(<MultiFundActionDialog fundIds={[1, 2, 3]} />);

        // single rule set -> straight to the action picker (a combobox)
        fireEvent.click(await screen.findByRole('combobox'));
        fireEvent.click(await screen.findByRole('option', { name: 'Migrace obsahu' }));
        fireEvent.click(screen.getByRole('button', { name: 'Pokračovat' }));

        // confirmation step exposes the "Spustit" button
        fireEvent.click(await screen.findByRole('button', { name: 'Spustit' }));

        // result step exposes "Zavřít" once the queue call resolved
        await screen.findByRole('button', { name: 'Zavřít' });

        expect(queueBody).toEqual({ fundVersionIds: [10, 20, 30], code: 'ContentMigration' });
    });

    it('shows the rule-set chooser with fund counts when funds span multiple rule sets', async () => {
        server.use(
            http.post('/api/v1/action/funds/group', () =>
                HttpResponse.json({
                    groups: [
                        {
                            ruleSetId: 1,
                            ruleSetCode: 'A',
                            ruleSetName: 'Pravidla A',
                            fundCount: 2,
                            fundVersionIds: [10, 20],
                            actions: [{ code: 'X', name: 'Akce X', fastAction: false }],
                        },
                        {
                            ruleSetId: 2,
                            ruleSetCode: 'B',
                            ruleSetName: 'Pravidla B',
                            fundCount: 1,
                            fundVersionIds: [30],
                            actions: [{ code: 'Y', name: 'Akce Y', fastAction: false }],
                        },
                    ],
                    skipped: [{ id: 99, reason: 'NO_OPEN_VERSION' }],
                }),
            ),
        );

        renderWithProviders(<MultiFundActionDialog fundIds={[1, 2, 3, 99]} />);

        expect(await screen.findByText('Pravidla A (2 fondů)')).toBeInTheDocument();
        expect(screen.getByText('Pravidla B (1 fondů)')).toBeInTheDocument();
    });
});
