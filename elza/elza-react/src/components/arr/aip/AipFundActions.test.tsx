import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ReactElement } from 'react';
import { Route } from 'react-router-dom';

import { renderWithProviders, screen, createTestStore, fireEvent, act } from 'test/test-utils';
import { AipLinkState } from 'elza-api';
import { AipFundActions } from './AipFundActions';

/**
 * Akce nad seznamem balíčků archivního souboru: popisek říká, s kterými balíčky akce pracuje
 * a kolik jich je, a připojení jednotlivě je dostupné jen pro jeden balíček.
 */

const api = vi.hoisted(() => ({ aipBulkConnectToJp: vi.fn(), aipConnectCheck: vi.fn() }));
vi.mock('../../../api', () => ({ Api: { aips: api } }));

vi.mock('components/shared/web-socket/WebsocketProvider', () => ({ useWebsocket: () => ({}) }));

// akce se provede hned - test ověřuje, která se zvolí, ne její průběh
vi.mock('../../aip/AipActionRunner', () => ({
    runAipAction: (_d: unknown, _i: unknown, _w: unknown, _t: unknown, request: () => unknown) => request(),
}));

// dialog se v testu vykreslí zvlášť; výběr jednotky popisu zastoupí tlačítko, které vybere uzel 10
const dialog = vi.hoisted(() => ({ content: null as unknown }));
vi.mock('actions/global/modalDialog', () => ({
    modalDialogShow: (_c: unknown, _title: unknown, content: unknown) => {
        dialog.content = content;
        return { type: 'test/noop' };
    },
    modalDialogHide: () => ({ type: 'test/noop' }),
}));
vi.mock('components/arr/FundNodesSelectForm', () => ({
    default: ({ onSubmitForm }: { onSubmitForm: (id: number) => void }) =>
        <button onClick={() => onSubmitForm(10)}>vybrat uzel</button>,
}));

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

const fund = { id: 5, fundTree: { nodes: [{ id: 1, name: 'Fond' }] } };

const button = (name: RegExp) => screen.getByRole('button', { name });

describe('AipFundActions', () => {
    beforeEach(() => {
        api.aipBulkConnectToJp.mockReset();
        api.aipConnectCheck.mockReset();
        api.aipConnectCheck.mockResolvedValue({ data: { blocked: [] } });
        dialog.content = null;
    });

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

    it('opens the bulk connection page with the packages it acts on', () => {
        renderWithProviders(<>
            <AipFundActions fund={fund} readMode={false} />
            <Route path="*" render={({ location }) => <output>{location.pathname + location.search}</output>} />
        </>, { preloadedState: state([aip(1), aip(2), aip(3)], [aip(1), aip(3)]) });

        fireEvent.click(button(/Připojit vybrané \(2\)/));

        expect(screen.getByRole('status').textContent).toMatch(/\/5\/aip\/connect\?aips=1,3$/);
    });

    it('opens the connection in the explorer of the package', () => {
        renderWithProviders(<>
            <AipFundActions fund={fund} readMode={false} />
            <Route path="*" render={({ location }) => <output>{location.pathname + location.search}</output>} />
        </>, { preloadedState: state([aip(1), aip(2)], [aip(2)]) });

        fireEvent.click(button(/Připojit jednotlivě/));

        expect(screen.getByRole('status').textContent).toMatch(/\/5\/aip\/2\/explorer\?tab=connect$/);
    });

    it('connects individually the package open in the detail when nothing is selected', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1)], [], 1) });

        expect(button(/Připojit jednotlivě/)).not.toHaveAttribute('aria-disabled', 'true');
    });

    it('quickly links whole packages to the unit picked in a dialog, staying in the list', async () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1), aip(2), aip(3)], [aip(1), aip(3)]) });

        fireEvent.click(button(/Připojit celé k JP/));
        renderWithProviders(dialog.content as ReactElement);
        await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'vybrat uzel' })); });

        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [1, 3]);
    });

    it('leaves out fully linked packages, and those the server would refuse, from the quick connect', async () => {
        const linked = { ...aip(2), linkState: AipLinkState.FullyLinked };
        api.aipConnectCheck.mockResolvedValue({ data: { blocked: [{ aipId: 3, reason: 'už napojen' }] } });
        renderWithProviders(<AipFundActions fund={fund} readMode={false} />,
            { preloadedState: state([aip(1), linked, aip(3)], []) });

        fireEvent.click(button(/Připojit celé k JP/));
        renderWithProviders(dialog.content as ReactElement);
        await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'vybrat uzel' })); });

        expect(api.aipConnectCheck).toHaveBeenCalledWith(10, [1, 3]);
        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [1]);
    });

    it('offers nothing in read mode', () => {
        renderWithProviders(<AipFundActions fund={fund} readMode={true} />,
            { preloadedState: state([aip(1)], [aip(1)]) });

        expect(button(/Připojit vybrané/)).toHaveAttribute('aria-disabled', 'true');
        expect(button(/Připojit jednotlivě/)).toHaveAttribute('aria-disabled', 'true');
        expect(button(/Připojit celé k JP/)).toHaveAttribute('aria-disabled', 'true');
    });
});
