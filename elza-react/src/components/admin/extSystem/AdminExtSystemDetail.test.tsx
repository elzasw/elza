import { describe, it, expect, vi, beforeEach } from 'vitest';

import { renderWithProviders, screen, fireEvent, act } from 'test/test-utils';
import { extSystemDetailFetchIfNeeded } from 'actions/admin/extSystem.jsx';
import { DaDownloadMethod, DaOnReceivedAction, DigitalRepositoryType, JAVA_ATTR_CLASS } from '../../../constants';
import { AdminExtSystemDetail } from './AdminExtSystemDetail';
import { EXT_SYSTEM_CLASS } from './ExtSystemForm';

/**
 * Detail of an external system: the settings of a digital archive and the test of its
 * configuration.
 */

const webApi = vi.hoisted(() => ({ getExtSystem: vi.fn(), getAllScopes: vi.fn() }));
vi.mock('actions/index.jsx', () => ({ WebApi: webApi }));

const api = vi.hoisted(() => ({ externalSystemTestDigitalRepository: vi.fn() }));
vi.mock('api', () => ({ Api: { externalSystems: api, aiprovider: {} } }));

const da = (id: number, extra: Record<string, unknown> = {}) => ({
    [JAVA_ATTR_CLASS]: EXT_SYSTEM_CLASS.ArrDigitalRepository,
    id,
    code: `DA${id}`,
    name: `Archiv ${id}`,
    url: 'http://da:8096/',
    digitalRepositoryType: DigitalRepositoryType.Da,
    sendNotification: false,
    multipleLinks: false,
    downloadMethod: DaDownloadMethod.FileTransfer,
    onReceived: DaOnReceivedAction.None,
    syncDelay: 300,
    statusPollInterval: 2,
    ...extra,
});

const systems: Record<number, unknown> = {};

async function renderDetail(id: number) {
    const view = renderWithProviders(<AdminExtSystemDetail />);
    await act(async () => {
        await (view.store.dispatch(extSystemDetailFetchIfNeeded(id) as never) as unknown as Promise<unknown>);
    });
    return view;
}

describe('AdminExtSystemDetail', () => {
    beforeEach(() => {
        webApi.getExtSystem.mockReset().mockImplementation((id: number) => Promise.resolve(systems[id]));
        webApi.getAllScopes.mockReset().mockResolvedValue([]);
        api.externalSystemTestDigitalRepository.mockReset();
    });

    it('shows the File Transfer endpoint derived from the URL and the configured type', async () => {
        systems[7] = da(7, { fileTransferType: 'GetMetadata_ELZA' });
        await renderDetail(7);

        expect(await screen.findByText('http://da:8096/ws/ft (odvozeno z URL)')).toBeInTheDocument();
        expect(screen.getByText('GetMetadata_ELZA')).toBeInTheDocument();
    });

    it('shows a configured File Transfer endpoint as it is', async () => {
        systems[7] = da(7, { fileTransferUrl: 'https://ft.example/transfer' });
        await renderDetail(7);

        expect(await screen.findByText('https://ft.example/transfer')).toBeInTheDocument();
        expect(screen.queryByText(/odvozeno z URL/)).toBeNull();
    });

    it('tests the configuration and shows the result', async () => {
        systems[7] = da(7);
        api.externalSystemTestDigitalRepository.mockResolvedValue({
            data: { available: true, templated: false, path: 'http://da:8096', message: 'DA API responds' },
        });
        await renderDetail(7);

        fireEvent.click(await screen.findByRole('button', { name: 'Vyzkoušet nastavení' }));

        expect(await screen.findByText('Repozitář je dostupný')).toBeInTheDocument();
        expect(screen.getByText('DA API responds')).toBeInTheDocument();
        expect(api.externalSystemTestDigitalRepository).toHaveBeenCalledWith(7);
    });

    it('does not show the test result of a system no longer selected', async () => {
        systems[7] = da(7);
        systems[8] = da(8);
        api.externalSystemTestDigitalRepository.mockResolvedValue({
            data: { available: true, templated: false, message: 'DA API responds' },
        });
        const { store } = await renderDetail(7);
        fireEvent.click(await screen.findByRole('button', { name: 'Vyzkoušet nastavení' }));
        expect(await screen.findByText('Repozitář je dostupný')).toBeInTheDocument();

        await act(async () => {
            await (store.dispatch(extSystemDetailFetchIfNeeded(8) as never) as unknown as Promise<unknown>);
        });

        expect(await screen.findByText('Archiv 8')).toBeInTheDocument();
        expect(screen.queryByText('Repozitář je dostupný')).toBeNull();
    });
});
