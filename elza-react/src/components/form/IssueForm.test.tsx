import { describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';

import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { IssueVO } from 'types';
import { IssueForm, IssueFormProps, IssueFormValues } from './IssueForm';

/**
 * Formulář připomínky lektorování: protokoly aktivního AS a typy připomínek se načtou
 * při otevření, typ a text jsou povinné.
 */

const FUND_ID = 3;

const preloadedState = {
    arrRegion: { activeIndex: 0, funds: [{ id: FUND_ID }] },
};

const savedIssue = { id: 99, issueListId: 11, issueTypeId: 3, description: 'Chybí datace' } as IssueVO;

function serveCodelists() {
    server.use(
        http.get('*/issue/issue_types', () =>
            HttpResponse.json([
                { id: 2, code: 'IMPORTANT', name: 'Závažná' },
                { id: 3, code: 'MINOR', name: 'Drobná' },
            ])
        ),
        http.get(`*/issue/funds/${FUND_ID}/issue_lists`, () =>
            HttpResponse.json([
                { id: 11, fundId: FUND_ID, name: 'Protokol A', open: true, rdUsers: [], wrUsers: [] },
                { id: 12, fundId: FUND_ID, name: 'Protokol B', open: true, rdUsers: [], wrUsers: [] },
            ])
        )
    );
}

function renderForm(props: Partial<IssueFormProps> = {}) {
    serveCodelists();
    const onSubmit = vi.fn<[IssueFormValues], Promise<IssueVO>>().mockResolvedValue(savedIssue);
    const onSubmitSuccess = vi.fn();
    const onClose = vi.fn();
    renderWithProviders(
        <IssueForm
            initialValues={{ issueListId: 11, issueTypeId: 2 }}
            onSubmit={onSubmit}
            onSubmitSuccess={onSubmitSuccess}
            onClose={onClose}
            {...props}
        />,
        { preloadedState }
    );
    return { onSubmit, onSubmitSuccess, onClose };
}

const protocolSelect = () => screen.getByLabelText<HTMLSelectElement>(/Protokol/);
const typeSelect = () => screen.getByLabelText<HTMLSelectElement>(/Typ připomínky/);
const textArea = () => screen.getByLabelText<HTMLTextAreaElement>(/Text připomínky/);

describe('IssueForm', () => {
    it('načte protokoly a typy a vybere výchozí hodnoty', async () => {
        renderForm();

        expect(await screen.findByRole('option', { name: 'Protokol B' })).toBeInTheDocument();
        expect(await screen.findByRole('option', { name: 'Drobná' })).toBeInTheDocument();
        expect(protocolSelect().value).toBe('11');
        expect(typeSelect().value).toBe('2');
        expect(textArea().value).toBe('');
    });

    it('bez textu připomínky neodešle a zobrazí chybu', async () => {
        const { onSubmit } = renderForm();
        await screen.findByRole('option', { name: 'Drobná' });

        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        expect(await screen.findByText('Pole je povinné')).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('odešle vyplněné hodnoty a předá uloženou připomínku dál', async () => {
        const { onSubmit, onSubmitSuccess } = renderForm();
        await screen.findByRole('option', { name: 'Drobná' });

        fireEvent.change(typeSelect(), { target: { value: '3' } });
        fireEvent.change(textArea(), { target: { value: 'Chybí datace' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        await vi.waitFor(() => expect(onSubmitSuccess).toHaveBeenCalledWith(savedIssue));
        expect(onSubmit).toHaveBeenCalledTimes(1);
        expect(onSubmit.mock.calls[0][0]).toMatchObject({
            issueListId: 11,
            issueTypeId: 3,
            description: 'Chybí datace',
        });
    });

    it('při úpravě nabízí Uložit a Zrušit formulář zavře', async () => {
        const { onClose } = renderForm({
            update: true,
            initialValues: { issueListId: 12, issueTypeId: 3, description: 'Původní text' },
        });
        await screen.findByRole('option', { name: 'Drobná' });

        expect(textArea().value).toBe('Původní text');
        expect(screen.getByRole('button', { name: 'Uložit' })).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Zrušit' }));

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
