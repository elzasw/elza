import { describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';

import { act, fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { IssueVO } from 'types';
import { IssueForm, IssueFormProps, IssueFormValues } from './IssueForm';

/**
 * Lecturing comment form: the protocols of the fund and the comment types load when it opens; protocol,
 * type and text are required.
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

function submitText(text: string) {
    fireEvent.change(textArea(), { target: { value: text } });
    fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));
}

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

        await vi.waitFor(() => expect(onSubmitSuccess).toHaveBeenCalledWith(savedIssue, { addAnother: false }));
        expect(onSubmit).toHaveBeenCalledTimes(1);
        expect(onSubmit.mock.calls[0][0]).toMatchObject({
            issueListId: 11,
            issueTypeId: 3,
            description: 'Chybí datace',
        });
    });

    it('protokol předaný jako text (hodnota ze selectu panelu lektorování) zůstane vybraný', async () => {
        const { onSubmit } = renderForm({
            initialValues: { issueListId: '12', issueTypeId: 2 } as unknown as IssueFormValues,
        });
        await screen.findByRole('option', { name: 'Protokol B' });

        expect(protocolSelect().value).toBe('12');

        fireEvent.change(textArea(), { target: { value: 'Do protokolu B' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        await vi.waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
        expect(Number(onSubmit.mock.calls[0][0].issueListId)).toBe(12);
    });

    it('během ukládání formulář překryje a zablokuje, po uložení ho uvolní', async () => {
        let finishSave: (issue: IssueVO) => void = () => undefined;
        const pendingSave = vi.fn<[IssueFormValues], Promise<IssueVO>>(
            () =>
                new Promise((resolve) => {
                    finishSave = resolve;
                })
        );
        renderForm({ onSubmit: pendingSave });
        await screen.findByRole('option', { name: 'Drobná' });

        fireEvent.change(textArea(), { target: { value: 'Pomalé uložení' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        expect(await screen.findByText('Ukládání…')).toBeInTheDocument();
        expect(textArea().closest('form')).toHaveAttribute('inert');

        await act(async () => finishSave(savedIssue));

        expect(screen.queryByText('Ukládání…')).not.toBeInTheDocument();
        expect(textArea().closest('form')).not.toHaveAttribute('inert');
    });

    it('neuložená připomínka zůstane ve formuláři s hláškou, po úspěšném uložení hláška zmizí', async () => {
        const failedRequest = { processed: true, createToaster: true };
        const onSubmit = vi
            .fn<[IssueFormValues], Promise<IssueVO>>()
            .mockRejectedValueOnce(failedRequest)
            .mockResolvedValueOnce(savedIssue);
        const { onSubmitSuccess } = renderForm({ onSubmit });
        await screen.findByRole('option', { name: 'Drobná' });

        submitText('Neuloženo');

        expect(await screen.findByText('Připomínku se nepodařilo uložit.')).toBeInTheDocument();
        expect(textArea().value).toBe('Neuloženo');
        expect(onSubmitSuccess).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        await vi.waitFor(() => expect(onSubmitSuccess).toHaveBeenCalledTimes(1));
        expect(screen.queryByText('Připomínku se nepodařilo uložit.')).not.toBeInTheDocument();
    });

    it('bez předvyplněného typu vybere první načtený typ', async () => {
        const { onSubmit } = renderForm({ initialValues: { issueListId: 11 } });
        await screen.findByRole('option', { name: 'Drobná' });

        await vi.waitFor(() => expect(typeSelect().value).toBe('2'));
        fireEvent.change(textArea(), { target: { value: 'Bez typu' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        await vi.waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
        expect(onSubmit.mock.calls[0][0]).toMatchObject({ issueTypeId: 2 });
    });

    it('nabídne jen protokoly, do kterých lze připomínku přidat, a jeden z nich vybere', async () => {
        renderForm({ canAddToProtocol: (protocol) => protocol.id === 12 });
        await screen.findByRole('option', { name: 'Protokol B' });

        expect(screen.queryByRole('option', { name: 'Protokol A' })).not.toBeInTheDocument();
        await vi.waitFor(() => expect(protocolSelect().value).toBe('12'));
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

    it('Přidat nechá formulář otevřený s prázdným textem, Přidat a zavřít ho dokončí', async () => {
        const { onSubmit, onSubmitSuccess } = renderForm({ canAddAnother: true });
        await screen.findByRole('option', { name: 'Drobná' });

        fireEvent.change(typeSelect(), { target: { value: '3' } });
        fireEvent.change(textArea(), { target: { value: 'První' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        await vi.waitFor(() => expect(onSubmitSuccess).toHaveBeenCalledWith(savedIssue, { addAnother: true }));
        expect(textArea().value).toBe('');
        await vi.waitFor(() => expect(textArea()).toHaveFocus());
        expect(typeSelect().value).toBe('3');
        expect(screen.queryByText('Pole je povinné')).not.toBeInTheDocument();

        fireEvent.change(textArea(), { target: { value: 'Druhá' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat a zavřít' }));

        await vi.waitFor(() => expect(onSubmitSuccess).toHaveBeenLastCalledWith(savedIssue, { addAnother: false }));
        expect(onSubmit.mock.calls[1][0]).toMatchObject({ issueTypeId: 3, description: 'Druhá' });
    });
});
