import { describe, it, expect } from 'vitest';

import { renderWithProviders, screen, createTestStore } from 'test/test-utils';
import { AipFundActions } from './AipFundActions';

/**
 * Akce nad seznamem balíčků archivního souboru: popisek říká, s kterými balíčky akce pracuje
 * a kolik jich je, a připojení jednotlivě je dostupné jen pro jeden balíček.
 */

const aip = (aipId: number) => ({ aipId, code: `AIP-${aipId}` });

/** Stav s balíčky zobrazenými na stránce seznamu a vybranými řádky. */
const state = (shown: unknown[], selected: unknown[], openAipId?: number) => {
    const base = createTestStore().getState() as { app: Record<string, object> };
    return {
        ...base,
        app: {
            ...base.app,
            aipList: { ...base.app.aipList, fetched: true, rows: shown, count: 10 },
            selectedAips: { ...base.app.selectedAips, rows: selected, count: selected.length },
            aip: openAipId != null ? { ...base.app.aip, id: openAipId } : base.app.aip,
        },
    };
};

const fund = { fundTree: { nodes: [{ id: 1 }] } };

const button = (name: RegExp) => screen.getByRole('button', { name });

describe('AipFundActions', () => {
    it('acts on the shown packages when nothing is selected - and says how many there are', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1), aip(2), aip(3)], []) });

        // the count is of the rows the action gets, not of all the pages of the list
        expect(button(/Připojit zobrazené \(3\)/)).toBeEnabled();
    });

    it('acts on the selected packages', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1), aip(2), aip(3)], [aip(2), aip(3)]) });

        expect(button(/Připojit vybrané \(2\)/)).toBeEnabled();
    });

    it('connects individually only one package', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1), aip(2)], [aip(1), aip(2)]) });

        expect(button(/Připojit jednotlivě/)).toHaveAttribute('aria-disabled', 'true');
    });

    it('connects individually the single selected package', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1), aip(2)], [aip(2)]) });

        expect(button(/Připojit jednotlivě/)).not.toHaveAttribute('aria-disabled', 'true');
    });

    it('connects individually the package open in the detail when nothing is selected', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1)], [], 1) });

        expect(button(/Připojit jednotlivě/)).not.toHaveAttribute('aria-disabled', 'true');
    });

    it('offers nothing in read mode', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={true} />,
            { preloadedState: state([aip(1)], [aip(1)]) });

        expect(button(/Připojit vybrané/)).toHaveAttribute('aria-disabled', 'true');
        expect(button(/Připojit jednotlivě/)).toHaveAttribute('aria-disabled', 'true');
    });
});
