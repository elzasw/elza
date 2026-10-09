import { describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';

import { fireEvent, renderWithProviders, screen, waitFor } from 'test/test-utils';
import { server } from 'test/mocks/server';
import ApStateChangeDialog from './ApStateChangeDialog';
import RevStateChangeDialog from './RevStateChangeDialog';
import { StateApproval } from 'api/StateApproval';
import { RevStateApproval } from 'api/RevStateApproval';

/**
 * Change of the state of an entity and of its revision: Fluent dialogs throughout, no bootstrap modal.
 */

const preloadedState = {
    refTables: {
        scopesData: {
            scopes: [
                {
                    versionId: -1,
                    isFetching: false,
                    isDirty: false,
                    scopes: [
                        { id: 7, name: 'ISAAR-CPF' },
                        { id: 8, name: 'CAM' },
                    ],
                },
            ],
        },
    },
    userDetail: { id: 1, permissionsMap: {}, userPermissions: [] as unknown[] },
    app: { apValidation: { data: { errors: [] as string[], partErrors: [] as unknown[] } } },
};

const users = [
    { id: 1, username: 'me', accessPoint: { name: 'Já' } },
    { id: 5, username: 'pepa', accessPoint: { name: 'Pepa' } },
];

const serve = (nextStates: StateApproval[]) => {
    server.use(
        http.get('*/registry/42/nextStates', () => HttpResponse.json(nextStates)),
        http.get('*/accesspoint/42/lastParticipants', () =>
            HttpResponse.json([{ userId: 5, name: 'Pepa', username: 'pepa' }])
        ),
        http.get('/api/registry/recordTypes', () =>
            HttpResponse.json([{ id: 2, code: 'PERSON', name: 'Person', addRecord: true, children: [] as unknown[] }])
        ),
        http.get('/api/user', () => HttpResponse.json({ rows: users, count: users.length })),
        http.get('/api/user/:id', ({ params }) => HttpResponse.json(users.find((u) => String(u.id) === params.id)))
    );
};

const renderDialog = (
    state: StateApproval,
    onSubmit = vi.fn(),
    nextStates = [StateApproval.NEW, StateApproval.TO_APPROVE]
) => {
    serve(nextStates);
    return renderWithProviders(
        <ApStateChangeDialog
            title="Změna vlastností"
            accessPointId={42}
            initialValues={{ state, typeId: 2, scopeId: 7 }}
            onClose={vi.fn()}
            onSubmit={onSubmit}
        />,
        { preloadedState }
    );
};

describe('ApStateChangeDialog', () => {
    it('is a Fluent dialog with Fluent fields only', async () => {
        renderDialog(StateApproval.NEW);

        expect(await screen.findByRole('dialog')).toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: /Oblast/ })).toHaveTextContent('ISAAR-CPF');
        expect(screen.getByRole('combobox', { name: /Stav/ })).toHaveTextContent('Nová');
        expect(screen.getByRole('combobox', { name: /Přiděleno/ })).toBeInTheDocument();
        expect(document.querySelector('.modal, .form-control, .form-group, .btn')).toBeNull();
    });

    it('assigns a last participant in one click and submits it', async () => {
        const onSubmit = vi.fn();
        renderDialog(StateApproval.NEW, onSubmit);

        fireEvent.click(await screen.findByRole('button', { name: 'Pepa (pepa)' }));
        await waitFor(() => expect(screen.getByRole('combobox', { name: /Přiděleno/ })).toHaveValue('Pepa (pepa)'));

        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));
        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0][0]).toMatchObject({ state: StateApproval.NEW, assignedTo: 5, scopeId: 7 });
    });

    it('leaves the state to choose when the user may approve an entity waiting for approval', async () => {
        renderDialog(StateApproval.TO_APPROVE, vi.fn(), [StateApproval.TO_APPROVE, StateApproval.APPROVED]);

        // the current state is only a placeholder
        fireEvent.click(await screen.findByRole('combobox', { name: /Stav/ }));
        expect(await screen.findByRole('option', { name: 'Ke schválení' })).toHaveAttribute('aria-selected', 'false');
        expect(screen.getByRole('button', { name: 'Uložit' })).toBeDisabled();

        fireEvent.click(screen.getByRole('option', { name: 'Schválená' }));
        await waitFor(() => expect(screen.getByRole('button', { name: 'Uložit' })).toBeEnabled());
    });

    it('drops the assignment of an approved entity', async () => {
        const onSubmit = vi.fn();
        renderDialog(StateApproval.NEW, onSubmit, [StateApproval.NEW, StateApproval.APPROVED]);

        fireEvent.click(await screen.findByRole('combobox', { name: /Stav/ }));
        fireEvent.click(await screen.findByRole('option', { name: 'Schválená' }));
        expect(screen.queryByRole('combobox', { name: /Přiděleno/ })).toBeNull();

        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));
        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0][0]).toMatchObject({ state: StateApproval.APPROVED, assignedTo: undefined });
    });
});

describe('RevStateChangeDialog', () => {
    it('is a Fluent dialog and does not let the user assign approval to themselves', async () => {
        serve([]);
        const onSubmit = vi.fn();
        renderWithProviders(
            <RevStateChangeDialog
                title="Změna stavu revize"
                accessPointId={42}
                scopeId={7}
                initialValues={{ state: RevStateApproval.ACTIVE as never, assignedTo: 1 }}
                onClose={vi.fn()}
                onSubmit={onSubmit}
            />,
            { preloadedState }
        );

        expect(await screen.findByRole('dialog')).toBeInTheDocument();
        expect(document.querySelector('.modal, .form-control, .form-group, .btn')).toBeNull();

        fireEvent.click(screen.getByRole('combobox', { name: /Stav/ }));
        fireEvent.click(await screen.findByRole('option', { name: 'Revize ke schválení' }));
        expect(await screen.findByText('Záznam může schválit pouze jiný uživatel')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Uložit' })).toBeDisabled();
    });
});
