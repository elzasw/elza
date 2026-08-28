import { describe, it, expect, vi, beforeEach } from 'vitest';

import { renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
import PackageBrowser from './PackageBrowser';

/**
 * Prohlížeč balíčku musí fungovat nezávisle na zpracování AIPu - právě u balíčku,
 * jehož zpracování selhalo, se do něj uživatel potřebuje podívat.
 */

const listPackageEntries = vi.fn();

vi.mock('../../../api', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../../api')>();
    return {
        ...actual,
        serverContextPath: '',
        Api: {aips: {aipListPackageEntries: (...args: unknown[]) => listPackageEntries(...args)}},
    };
});

beforeEach(() => {
    listPackageEntries.mockReset();
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({text: () => Promise.resolve('<mets/>')})));
});

describe('PackageBrowser', () => {
    it('vypíše soubory balíčku', async () => {
        listPackageEntries.mockResolvedValue({data: [
            {path: 'aip/METS.xml', size: 2048},
            {path: 'aip/data/scan.jpg', size: 1024},
        ]});

        renderWithProviders(<PackageBrowser aipId={11}/>);

        expect(await screen.findByText('aip/METS.xml')).toBeInTheDocument();
        expect(screen.getByText('aip/data/scan.jpg')).toBeInTheDocument();
    });

    it('u nestaženého balíčku vysvětlí, proč není co ukázat', async () => {
        listPackageEntries.mockRejectedValue(new Error('404'));

        renderWithProviders(<PackageBrowser aipId={11}/>);

        expect(await screen.findByText('Pro tento AIP není stažený žádný balíček.')).toBeInTheDocument();
    });

    it('vybraný XML soubor zobrazí jako text', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/METS.xml', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}/>);
        fireEvent.click(await screen.findByText('aip/METS.xml'));

        await waitFor(() => expect(screen.getByText('<mets/>')).toBeInTheDocument());
        expect(fetch).toHaveBeenCalledWith('/api/v1/aip/11/package/content?path=aip%2FMETS.xml');
    });

    it('binární soubor nabídne jen ke stažení', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/data/scan.jpg', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}/>);
        fireEvent.click(await screen.findByText('aip/data/scan.jpg'));

        expect(await screen.findByText('Soubor nelze zobrazit jako text, lze jej stáhnout.')).toBeInTheDocument();
        expect(fetch).not.toHaveBeenCalled();
    });
});
