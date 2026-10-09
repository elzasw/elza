import { describe, it, expect, vi, beforeEach } from 'vitest';

import { renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
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
        renderWithProviders(<DaQueueList repositoryId={7} />);

        expect(await screen.findByText('aip-2')).toBeInTheDocument();
        expect(api.externalSystemDaQueue).toHaveBeenCalledWith(7, false, undefined, undefined, undefined, undefined,
                                                               false, 0, 50);
    });

    it('offers retry but not cancel for an export the DA received', async () => {
        serve([item(1, QueueItemState.ExportSent, { batchId: 'e1' })]);
        renderWithProviders(<DaQueueList repositoryId={7} />);

        await select('aip-1');

        expect(button(/Zkusit znovu teď/)).toBeEnabled();
        expect(button(/Zrušit požadavek/)).toBeDisabled();
        expect(button(/Opakovat/)).toBeDisabled();
    });

    it('offers repeat for a failed request', async () => {
        serve([item(1, QueueItemState.ImportError)]);
        renderWithProviders(<DaQueueList repositoryId={7} />);

        await select('aip-1');

        expect(button(/Opakovat/)).toBeEnabled();
        expect(button(/Zkusit znovu teď/)).toBeDisabled();
    });

    it('cancels the selected requests and says what it did', async () => {
        serve([item(1, QueueItemState.Update), item(2, QueueItemState.ExportNew)]);
        api.externalSystemDaQueueWithdraw.mockResolvedValue({ data: { done: 2, skipped: 0 } });
        renderWithProviders(<DaQueueList repositoryId={7} />);

        await select('aip-1');
        await select('aip-2');
        fireEvent.click(button(/Zrušit požadavek/));

        await waitFor(() => expect(toasts.success).toHaveBeenCalledWith('Provedeno u 2 požadavků'));
        expect(api.externalSystemDaQueueWithdraw).toHaveBeenCalledWith(7, [1, 2]);
    });

    it('filters the whole history by a batch clicked in the list', async () => {
        serve([item(1, QueueItemState.DownloadRequested, { batchId: 'd1' })]);
        renderWithProviders(<DaQueueList repositoryId={7} />);

        fireEvent.click(await screen.findByRole('button', { name: 'd1' }));

        await waitFor(() => expect(api.externalSystemDaQueue).toHaveBeenLastCalledWith(
            7, true, undefined, undefined, undefined, 'd1', false, 0, 50));
    });
});
