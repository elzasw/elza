import { describe, it, expect, vi, beforeEach } from 'vitest';

import { renderWithProviders, screen, fireEvent, waitFor, within } from 'test/test-utils';
import { FluentDialogProvider } from 'components/shared/dialog/FluentModalDialog';
import { DaQueueDirection, DaQueueItemVO, QueueItemState } from 'elza-api';
import { DaQueueList } from './DaQueueList';

/**
 * The queue of a digital archive: waiting requests first, the actions over the selection are
 * offered only when they fit at least one selected request.
 */

const api = vi.hoisted(() => ({
    externalSystemDaQueue: vi.fn(),
    externalSystemDaQueueRetryNow: vi.fn(),
    externalSystemDaQueueWithdraw: vi.fn(),
    externalSystemDaQueueRepeat: vi.fn(),
    externalSystemDaQueueMarkFailed: vi.fn(),
}));
vi.mock('api', () => ({ Api: { externalSystems: api } }));

const toasts = vi.hoisted(() => ({ success: vi.fn(), warning: vi.fn() }));
vi.mock('../../shared/toastr/ToastrActions', () => ({
    addToastrSuccess: (title: string) => { toasts.success(title); return { type: 'test/noop' }; },
    addToastrWarning: (title: string) => { toasts.warning(title); return { type: 'test/noop' }; },
}));

const EXPORT_STATES: QueueItemState[] = [
    QueueItemState.ExportNew, QueueItemState.ExportSent, QueueItemState.ExportOk, QueueItemState.ExportError,
];

const item = (id: number, state: QueueItemState, extra: Partial<DaQueueItemVO> = {}): DaQueueItemVO => ({
    id,
    aipCode: `aip-${id}`,
    direction: EXPORT_STATES.includes(state) ? DaQueueDirection.Export : DaQueueDirection.Download,
    state,
    active: true,
    attemptCount: 0,
    ...extra,
});

const serve = (items: DaQueueItemVO[]) =>
    api.externalSystemDaQueue.mockResolvedValue({ data: { items, totalCount: items.length } });

const button = (name: RegExp) => screen.getByRole('button', { name });
// the list asks for confirmations through the dialog provider the application layout supplies
const renderQueue = () =>
    renderWithProviders(<FluentDialogProvider hidden={false}><DaQueueList repositoryId={7} /></FluentDialogProvider>);
const select = async (code: string) => {
    const row = (await screen.findByText(code)).closest('tr')!;
    fireEvent.click(row.querySelector('[role="checkbox"], input[type="checkbox"]')!);
};

describe('DaQueueList', () => {
    beforeEach(() => {
        Object.values(api).forEach(fn => fn.mockReset());
        toasts.success.mockReset();
        toasts.warning.mockReset();
    });

    it('asks for the waiting requests of the repository, newest page first', async () => {
        serve([item(2, QueueItemState.Update)]);
        renderQueue();

        expect(await screen.findByText('aip-2')).toBeInTheDocument();
        expect(api.externalSystemDaQueue).toHaveBeenCalledWith(7, false, undefined, undefined, undefined, undefined,
                                                               false, 0, 50);
    });

    it('offers retry but not cancel for an export the DA received', async () => {
        serve([item(1, QueueItemState.ExportSent, { batchId: 'e1' })]);
        renderQueue();

        await select('aip-1');

        expect(button(/Zkusit znovu teď/)).toBeEnabled();
        expect(button(/Zrušit požadavek/)).toBeDisabled();
        expect(button(/Opakovat/)).toBeDisabled();
    });

    it('offers repeat for a failed request', async () => {
        serve([item(1, QueueItemState.ImportError)]);
        renderQueue();

        await select('aip-1');

        expect(button(/Opakovat/)).toBeEnabled();
        expect(button(/Zkusit znovu teď/)).toBeDisabled();
    });

    it('cancels the selected requests and says what it did', async () => {
        serve([item(1, QueueItemState.Update), item(2, QueueItemState.ExportNew)]);
        api.externalSystemDaQueueWithdraw.mockResolvedValue({ data: { done: 2, skipped: 0 } });
        renderQueue();

        await select('aip-1');
        await select('aip-2');
        fireEvent.click(button(/Zrušit požadavek/));

        await waitFor(() => expect(toasts.success).toHaveBeenCalledWith('Provedeno u 2 požadavků'));
        expect(api.externalSystemDaQueueWithdraw).toHaveBeenCalledWith(7, [1, 2]);
    });

    const failing = () => item(1, QueueItemState.DownloadRequested,
                               { batchId: 'd1', attemptCount: 3, failingSince: '2026-10-25T18:46:20Z' });
    it('marks a download that keeps failing as failed from its detail, once confirmed', async () => {
        serve([failing()]);
        api.externalSystemDaQueueMarkFailed.mockResolvedValue({ data: { done: 1, skipped: 0 } });
        renderQueue();

        fireEvent.click(await screen.findByText('aip-1'));
        fireEvent.click(button(/Označit jako chybné/));
        const dialog = await screen.findByRole('alertdialog');
        expect(within(dialog).getByText(/aip-1/)).toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('button', { name: /Označit jako chybné/ }));

        await waitFor(() => expect(api.externalSystemDaQueueMarkFailed).toHaveBeenCalledWith(7, [1]));
    });

    it('leaves the download alone when marking as failed is not confirmed', async () => {
        serve([failing()]);
        renderQueue();

        fireEvent.click(await screen.findByText('aip-1'));
        fireEvent.click(button(/Označit jako chybné/));
        const dialog = await screen.findByRole('alertdialog');
        fireEvent.click(within(dialog).getByRole('button', { name: /Zrušit/ }));

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
        expect(api.externalSystemDaQueueMarkFailed).not.toHaveBeenCalled();
    });

    it('does not offer marking as failed for a download that failed once but no longer fails', async () => {
        serve([item(1, QueueItemState.DownloadRequested, { batchId: 'd1', attemptCount: 1 })]);
        renderQueue();

        fireEvent.click(await screen.findByText('aip-1'));

        expect(await screen.findByText('Požadavek 1')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Označit jako chybné/ })).not.toBeInTheDocument();
    });

    it('filters the whole history by a batch clicked in the list', async () => {
        serve([item(1, QueueItemState.DownloadRequested, { batchId: 'd1' })]);
        renderQueue();

        fireEvent.click(await screen.findByRole('button', { name: 'd1' }));

        await waitFor(() => expect(api.externalSystemDaQueue).toHaveBeenLastCalledWith(
            7, true, undefined, undefined, undefined, 'd1', false, 0, 50));
    });

    it('reloads after a pause in typing, not on every key', async () => {
        serve([item(1, QueueItemState.Update)]);
        renderQueue();
        await screen.findByText('aip-1');
        const calls = api.externalSystemDaQueue.mock.calls.length;

        fireEvent.change(screen.getByRole('textbox', { name: 'Kód AIP' }), { target: { value: 'ab' } });
        fireEvent.change(screen.getByRole('textbox', { name: 'Kód AIP' }), { target: { value: 'abc' } });
        expect(api.externalSystemDaQueue.mock.calls.length).toBe(calls);

        await waitFor(() => expect(api.externalSystemDaQueue).toHaveBeenLastCalledWith(
            7, false, undefined, undefined, 'abc', undefined, false, 0, 50));
        expect(api.externalSystemDaQueue.mock.calls.length).toBe(calls + 1);
    });
});
