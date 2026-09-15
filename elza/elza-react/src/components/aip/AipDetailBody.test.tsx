import { describe, expect, it, vi } from 'vitest';
import { AipDetailVO, AipLinkState, AipProblemType, LinkType, QueueItemState } from 'elza-api';

import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { AipDetailBody } from './AipDetailBody';

/**
 * The detail panel is where the user reads what ELZA holds of an AIP: the queue state has to
 * read as words, a value the AIP does not have is not shown at all, and a problem gets its
 * own block with the description.
 */

const detail = (overrides: Partial<AipDetailVO> = {}) => ({
    aipId: 3,
    code: '9c26d4bb-cb5b-4007-84df-121be156f722',
    digitalRepositoryId: 1,
    aipVersion: '1',
    ...overrides,
} as AipDetailVO);

describe('AipDetailBody', () => {

    it('stav fronty vypíše slovy, ne jako kód stavu', () => {
        renderWithProviders(<AipDetailBody detail={detail({
            importState: QueueItemState.ImportError,
            importStateMessage: 'Balíček neobsahuje soubor PACKAGE-INFO.xml',
        })} />);

        expect(screen.getByText('Chyba stažení')).toBeInTheDocument();
        expect(screen.queryByText('IMPORT_ERROR')).toBeNull();
        expect(screen.getByText('Chyba stažení').closest('span')).toHaveAttribute(
            'title', expect.stringContaining('PACKAGE-INFO.xml'));
    });

    it('hodnotu, kterou AIP nemá, nevypíše vůbec', () => {
        renderWithProviders(<AipDetailBody detail={detail()} />);

        expect(screen.queryByText('Velikost')).toBeNull();
        expect(screen.queryByText('Datace od-do')).toBeNull();
        // the load flags are shown even when "no" - that is information too
        expect(screen.getByText('Načtená metadata')).toBeInTheDocument();
        expect(screen.getByText('Načtený kompletní AIP')).toBeInTheDocument();
    });

    it('problém dostane vlastní blok s popisem', () => {
        renderWithProviders(<AipDetailBody detail={detail({
            problemType: AipProblemType.MetadataError,
            problemDescription: 'Balíček neobsahuje soubor PACKAGE-INFO.xml',
        })} />);

        expect(screen.getByText('Chyba při zpracování metadat')).toBeInTheDocument();
        expect(screen.getByText('Balíček neobsahuje soubor PACKAGE-INFO.xml')).toBeInTheDocument();
    });

    it('fond odkazuje na balíčky svého archivního souboru, rovnou na tento AIP', () => {
        const { container } = renderWithProviders(<AipDetailBody detail={detail({
            fund: { id: 106, name: 'Fond A' } as AipDetailVO['fund'],
        })} />);

        const link = container.querySelector('a[href="/fund/106/aip/3"]');
        expect(link?.textContent).toBe('Fond A');
    });

    it('napojení a velikost vypíše přeloženě', () => {
        renderWithProviders(<AipDetailBody detail={detail({
            aipSize: 2048,
            linkState: AipLinkState.NotLinked,
        })} />);

        expect(screen.getByText('2.0 kB')).toBeInTheDocument();
        expect(screen.getByText('Nenapojeno')).toBeInTheDocument();
    });

    const fund = { id: 106, name: 'Fond A' } as AipDetailVO['fund'];

    it('napojení celého balíčku vypíše jménem, napojené části jen spočítá', () => {
        renderWithProviders(<AipDetailBody detail={detail({
            fund,
            linkedNodes: [
                { id: 1, nodeId: 10, name: 'Celý balíček', linkType: LinkType.Aip },
                { id: 2, nodeId: 11, name: 'Část A', linkType: LinkType.PartAip },
                { id: 3, nodeId: 12, name: 'Komponenta B', linkType: LinkType.ComponentAip },
            ],
        })} />);

        expect(screen.getByText('Celý balíček')).toBeInTheDocument();
        expect(screen.queryByText('Část A')).toBeNull();
        expect(screen.queryByText('Komponenta B')).toBeNull();
        expect(screen.getByText('Napojené části')).toBeInTheDocument();
        expect(screen.getByText(/\b2\b/)).toBeInTheDocument();
    });

    it('napojení bez typu bere jako napojení celého balíčku', () => {
        renderWithProviders(<AipDetailBody detail={detail({
            fund,
            linkedNodes: [{ id: 1, nodeId: 10, name: 'Starý link' }],
        })} />);

        expect(screen.getByText('Starý link')).toBeInTheDocument();
        expect(screen.queryByText('Napojené části')).toBeNull();
    });

    it('soubor problému nabídne k otevření jen tam, kde ho lze ukázat', () => {
        const problem = {
            problemType: AipProblemType.MetadataError,
            problemDescription: 'Popis se nepodařilo načíst.',
            problemFile: 'metadata/descriptive/pruvodka.xml',
        };
        const onOpenProblemFile = vi.fn();

        const { unmount } = renderWithProviders(<AipDetailBody detail={detail(problem)} />);
        expect(screen.queryByText('metadata/descriptive/pruvodka.xml')).toBeNull();
        unmount();

        renderWithProviders(<AipDetailBody detail={detail(problem)} onOpenProblemFile={onOpenProblemFile} />);
        fireEvent.click(screen.getByText('metadata/descriptive/pruvodka.xml'));

        expect(onOpenProblemFile).toHaveBeenCalledWith('metadata/descriptive/pruvodka.xml');
    });
});
