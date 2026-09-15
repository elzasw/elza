import { describe, it, expect, vi, beforeEach } from 'vitest';

import { AipProblemType } from 'elza-api';
import { AddSquare16Regular, SubtractSquare16Regular } from '@fluentui/react-icons';
import { render, renderWithProviders, screen, fireEvent, waitFor } from 'test/test-utils';
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

        // strom: složky a jména souborů, ne celé cesty
        expect(await screen.findByText('METS.xml')).toBeInTheDocument();
        expect(screen.getByText('data')).toBeInTheDocument();
        expect(screen.getByText('scan.jpg')).toBeInTheDocument();
    });

    it('ikona složky odpovídá tomu, zda je složka otevřená', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/data/scan.jpg', size: 1024}]});

        renderWithProviders(<PackageBrowser aipId={11}/>);

        const folder = (await screen.findByText('data')).closest('[role="treeitem"]')!;
        const expandIcon = () => folder.querySelector('.fui-TreeItemLayout__expandIcon')!.innerHTML;
        // markup ikon, aby se porovnávalo proti nim, ne proti natvrdo opsané kresbě
        const minus = render(<SubtractSquare16Regular color="black"/>).container.innerHTML;
        const plus = render(<AddSquare16Regular color="black"/>).container.innerHTML;

        // strom se otevírá celý, otevřená složka se nabízí ke sbalení
        expect(expandIcon()).toBe(minus);

        fireEvent.click(screen.getByText('data'));

        await waitFor(() => expect(folder).toHaveAttribute('aria-expanded', 'false'));
        // sbalená složka se naopak nabízí k rozbalení
        expect(expandIcon()).toBe(plus);
    });

    it('u nestaženého balíčku vysvětlí, proč není co ukázat', async () => {
        listPackageEntries.mockRejectedValue(new Error('404'));

        renderWithProviders(<PackageBrowser aipId={11}/>);

        expect(await screen.findByText('Pro tento AIP není stažený žádný balíček.')).toBeInTheDocument();
    });

    it('vybraný XML soubor zobrazí jako text', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/METS.xml', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}/>);
        fireEvent.click(await screen.findByText('METS.xml'));

        await waitFor(() => expect(screen.getByText('<mets/>')).toBeInTheDocument());
        expect(fetch).toHaveBeenCalledWith('/api/v1/aip/11/package/content?path=aip%2FMETS.xml');
    });

    it('binární soubor nabídne jen ke stažení', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/data/scan.jpg', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}/>);
        fireEvent.click(await screen.findByText('scan.jpg'));

        expect(await screen.findByText('Soubor nelze zobrazit jako text, lze jej stáhnout.')).toBeInTheDocument();
        expect(fetch).not.toHaveBeenCalled();
    });

    it('nabídne stažení celého balíčku', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/METS.xml', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}/>);

        const link = await screen.findByText('Stáhnout celý balíček');
        expect(link.closest('a')).toHaveAttribute('href', '/api/v1/aip/11/package/download');
    });

    it('ukáže problém AIPu nad obsahem balíčku', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/METS.xml', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}
                                            problemType={AipProblemType.MetadataError}
                                            problemDescription="Balíček neobsahuje soubor 'PREMIS.xml'"/>);

        expect(await screen.findByText('Chyba při zpracování metadat')).toBeInTheDocument();
        expect(screen.getByText("Balíček neobsahuje soubor 'PREMIS.xml'")).toBeInTheDocument();
    });

    it('soubor, kterého se problém týká, otevře kliknutím', async () => {
        listPackageEntries.mockResolvedValue({data: [
            {path: 'aip/METS.xml', size: 10},
            {path: 'aip/metadata/descriptive/pruvodka.xml', size: 20},
        ]});

        // balíček uvádí cestu vůči svému kořeni, v ZIPu je pod složkou pojmenovanou kódem AIPu
        renderWithProviders(<PackageBrowser aipId={11}
                                            problemType={AipProblemType.MetadataError}
                                            problemDescription="Popis se nepodařilo načíst."
                                            problemFile="metadata/descriptive/pruvodka.xml"/>);

        fireEvent.click(await screen.findByText('aip/metadata/descriptive/pruvodka.xml'));

        await waitFor(() => expect(fetch).toHaveBeenCalledWith(
            '/api/v1/aip/11/package/content?path=aip%2Fmetadata%2Fdescriptive%2Fpruvodka.xml'));
    });

    it('neodkazuje na soubor, který balíček neobsahuje', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/METS.xml', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11}
                                            problemType={AipProblemType.MetadataError}
                                            problemDescription="Balíček neobsahuje soubor EAD-INHERENT.xml"
                                            problemFile="metadata/descriptive/EAD-INHERENT.xml"/>);

        expect(await screen.findByText('Balíček neobsahuje soubor EAD-INHERENT.xml')).toBeInTheDocument();
        expect(screen.queryByText('Soubor, kterého se problém týká:')).not.toBeInTheDocument();
    });

    it('požadovaný soubor otevře, jakmile je balíček načtený', async () => {
        listPackageEntries.mockResolvedValue({data: [
            {path: 'aip/METS.xml', size: 10},
            {path: 'aip/metadata/descriptive/pruvodka.xml', size: 20},
        ]});

        renderWithProviders(<PackageBrowser aipId={11} selectPath="metadata/descriptive/pruvodka.xml"/>);

        await waitFor(() => expect(fetch).toHaveBeenCalledWith(
            '/api/v1/aip/11/package/content?path=aip%2Fmetadata%2Fdescriptive%2Fpruvodka.xml'));
    });

    it('soubor, který balíček neobsahuje, neotevře', async () => {
        listPackageEntries.mockResolvedValue({data: [{path: 'aip/METS.xml', size: 10}]});

        renderWithProviders(<PackageBrowser aipId={11} selectPath="metadata/descriptive/pruvodka.xml"/>);

        expect(await screen.findByText('METS.xml')).toBeInTheDocument();
        expect(fetch).not.toHaveBeenCalled();
        expect(screen.getByText('Vyberte soubor balíčku.')).toBeInTheDocument();
    });
});
