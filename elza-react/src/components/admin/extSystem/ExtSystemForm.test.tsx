import { describe, it, expect, vi } from 'vitest';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { JAVA_ATTR_CLASS, DigitalRepositoryType, DaDownloadMethod, DaOnReceivedAction } from '../../../constants';
import ExtSystemForm, { EXT_SYSTEM_CLASS } from './ExtSystemForm';

/**
 * Formulář externího systému: nastavení stahování AIP (způsob stahování, akce po
 * přijetí) se nabízí jen pro úložiště typu Digitální archiv.
 *
 * Legacy texty (i18n) nejsou v testu načtené a vrací '[klíč]'; react-intl texty se vykreslí
 * jako české defaultMessage. Pole se hledají podle atributu name.
 */

vi.mock('actions/index.jsx', () => ({
    WebApi: { getAllScopes: () => Promise.resolve([]) },
}));

const select = (container: HTMLElement, name: string) =>
    container.querySelector<HTMLSelectElement>(`select[name="${name}"]`);

const repository = (digitalRepositoryType: DigitalRepositoryType, extra: Record<string, unknown> = {}) => ({
    [JAVA_ATTR_CLASS]: EXT_SYSTEM_CLASS.ArrDigitalRepository,
    code: 'REPO',
    name: 'Repository',
    digitalRepositoryType,
    ...extra,
});

describe('ExtSystemForm - nastavení stahování AIP', () => {
    it('nabízí způsob stahování a akci po přijetí pro digitální archiv', () => {
        const { container } = renderWithProviders(
            <ExtSystemForm initialValues={repository(DigitalRepositoryType.Da)} onSubmitForm={vi.fn()} />,
        );

        expect(select(container, 'downloadMethod')).not.toBeNull();
        expect(select(container, 'onReceived')).not.toBeNull();
        expect(screen.getByText('Způsob stahování AIP')).toBeInTheDocument();
        expect(screen.getByText('Po přijetí AIP')).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Standardní (HTTP)' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'File Transfer' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Nic nedělat' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Stáhnout metadata' })).toBeInTheDocument();
    });

    it.each([DigitalRepositoryType.Filesystem, DigitalRepositoryType.Wsdl])(
        'skrývá nastavení stahování pro typ %s',
        (type) => {
            const { container } = renderWithProviders(
                <ExtSystemForm initialValues={repository(type)} onSubmitForm={vi.fn()} />,
            );

            expect(select(container, 'downloadMethod')).toBeNull();
            expect(select(container, 'onReceived')).toBeNull();
        },
    );

    it('uložená hodnota "Ne" (false) u zasílání upozornění není považována za nevyplněnou', async () => {
        const onSubmitForm = vi.fn().mockResolvedValue(undefined);
        const { container } = renderWithProviders(
            <ExtSystemForm
                initialValues={repository(DigitalRepositoryType.Da, {
                    id: 7,
                    sendNotification: false,
                    downloadMethod: DaDownloadMethod.Standard,
                    onReceived: DaOnReceivedAction.None,
                })}
                onSubmitForm={onSubmitForm}
            />,
        );

        fireEvent.change(select(container, 'downloadMethod')!, { target: { value: DaDownloadMethod.FileTransfer } });
        fireEvent.click(screen.getByRole('button', { name: '[admin.extSystem.submit.edit]' }));

        await vi.waitFor(() => expect(onSubmitForm).toHaveBeenCalledTimes(1));
    });

    it('odešle zvolené hodnoty', async () => {
        const onSubmitForm = vi.fn().mockResolvedValue(undefined);
        const { container } = renderWithProviders(
            <ExtSystemForm
                initialValues={repository(DigitalRepositoryType.Da, {
                    id: 7,
                    sendNotification: 'false',
                    downloadMethod: DaDownloadMethod.Standard,
                    onReceived: DaOnReceivedAction.None,
                })}
                onSubmitForm={onSubmitForm}
            />,
        );

        fireEvent.change(select(container, 'downloadMethod')!, { target: { value: DaDownloadMethod.FileTransfer } });
        fireEvent.change(select(container, 'onReceived')!, { target: { value: DaOnReceivedAction.DownloadMetadata } });
        fireEvent.click(screen.getByRole('button', { name: '[admin.extSystem.submit.edit]' }));

        await vi.waitFor(() => expect(onSubmitForm).toHaveBeenCalledTimes(1));
        expect(onSubmitForm.mock.calls[0][0]).toMatchObject({
            downloadMethod: DaDownloadMethod.FileTransfer,
            onReceived: DaOnReceivedAction.DownloadMetadata,
        });
    });
});
