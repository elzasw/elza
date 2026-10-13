import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PublicationDetail } from 'elza-api';

import { renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
import { copyTextToClipboard } from 'utils/clipboard';
import { PublicationErrorDialog } from './PublicationErrorDialog';

vi.mock('utils/clipboard', () => ({
    copyTextToClipboard: vi.fn(async () => true),
}));

const publication = (errorMessage: string) => ({
    id: 103,
    typeName: 'Badatelna',
    errorAt: '2026-10-13T14:31:17.218+02:00',
    errorMessage,
}) as PublicationDetail;

describe('PublicationErrorDialog', () => {
    beforeEach(() => {
        vi.mocked(copyTextToClipboard).mockClear();
    });

    it('shows the whole message and copies it unchanged', async () => {
        const longMessage = 'Maximum call stack size exceeded\n    at parseLevels (import.js:42:17)\n' + 'x'.repeat(2000);
        renderWithProviders(<PublicationErrorDialog publication={publication(longMessage)} onClose={() => {}} />);

        expect(screen.getByText('Badatelna', { exact: false })).toBeInTheDocument();
        expect(screen.getByTestId('publication-error-text').textContent).toBe(longMessage);

        fireEvent.click(screen.getByRole('button', { name: 'Zkopírovat do schránky' }));
        await waitFor(() => expect(copyTextToClipboard).toHaveBeenCalledWith(longMessage));
    });

    it('indents a JSON message but copies the original text', async () => {
        const json = '{"type":"BaseCode","code":"SYSTEM_ERROR","message":"Požadovaný objekt nenalezen"}';
        renderWithProviders(<PublicationErrorDialog publication={publication(json)} onClose={() => {}} />);

        expect(screen.getByTestId('publication-error-text').textContent).toBe(JSON.stringify(JSON.parse(json), null, 2));

        fireEvent.click(screen.getByRole('button', { name: 'Zkopírovat do schránky' }));
        await waitFor(() => expect(copyTextToClipboard).toHaveBeenCalledWith(json));
    });

    it('is closed when there is no publication and reports closing', () => {
        const onClose = vi.fn();
        const { rerender } = renderWithProviders(<PublicationErrorDialog publication={null} onClose={onClose} />);
        expect(screen.queryByTestId('publication-error-text')).not.toBeInTheDocument();

        rerender(<PublicationErrorDialog publication={publication('boom')} onClose={onClose} />);
        fireEvent.click(screen.getByRole('button', { name: 'Zavřít' }));
        expect(onClose).toHaveBeenCalled();
    });
});
