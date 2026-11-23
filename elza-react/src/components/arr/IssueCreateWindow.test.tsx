import { describe, expect, it } from 'vitest';
import { delay, http, HttpResponse } from 'msw';
import { AnyAction, applyMiddleware, createStore } from 'redux';
import thunk from 'redux-thunk';

import { act, fireEvent, renderWithProviders, screen, waitFor, within } from 'test/test-utils';
import { server } from 'test/mocks/server';
import rootReducer from 'stores/reducers';
import { FluentDialogContext, FluentDialogProvider } from 'components/shared/dialog/FluentModalDialog';
import { IssueFormValues } from 'components/form/IssueForm';
import { UserProvider } from 'contexts/user';
import { useIssueCreateWindow } from './useIssueCreateWindow';
import { IssueTarget } from './IssueCreateWindow';
import { urlFund, urlFundNode, urlFundTree } from '../../constants';
import { useContext, useEffect } from 'react';
import { useHistory } from 'react-router-dom';

/**
 * Adding a lecturing comment in a non-modal window: the comment targets the fund or a unit of description
 * and follows the fund open in the URL and the selected unit until the user locks the unit. When the form
 * cannot be used (no fund open, locked unit from another fund, no permission), a message covers it. The
 * window cannot be resized, at most one is open, and the protocol always belongs to the open fund.
 */

const FUND_ID = 3;
const OTHER_FUND_ID = 4;
const initialValues: IssueFormValues = { issueListId: 11, issueTypeId: 2 };

const SET_ARR_REGION = 'TEST_SET_ARR_REGION';

const LOCKED_ELSEWHERE = 'Zamčená jednotka popisu je z jiného archivního souboru';
const UNLOCK = 'Odemknout a sledovat vybranou jednotku popisu';
const NO_PERMISSION = 'Nemáte oprávnění přidávat připomínky v tomto archivním souboru';
const NO_OPEN_PROTOCOL = 'V tomto archivním souboru nebyl nalezen žádný otevřený protokol';

interface TestArrRegion {
    activeIndex: number | null;
    funds: { id: number; nodes: { activeIndex: number | null; nodes: object[] } }[];
}

interface ArrRegionOptions {
    fundId?: number;
    selectedSubNodeId: number | null;
}

function arrRegion({ fundId = FUND_ID, selectedSubNodeId }: ArrRegionOptions): TestArrRegion {
    return {
        activeIndex: 0,
        funds: [
            {
                id: fundId,
                nodes: {
                    activeIndex: 0,
                    nodes: [
                        {
                            id: 100,
                            selectedSubNodeId,
                            childNodes: [
                                { id: 5, referenceMark: ['1', '2'], accordionLeft: 'Listina' },
                                { id: 6, referenceMark: ['1', '3'], accordionLeft: 'Kronika' },
                            ],
                        },
                    ],
                },
            },
        ],
    };
}

type RootState = ReturnType<typeof rootReducer>;

/** The real reducers plus a test action that replaces the arrangement state (open fund, selected unit). */
function testReducer(state: RootState | undefined, action: AnyAction): RootState {
    const nextState = rootReducer(state, action);
    if (action.type === SET_ARR_REGION) {
        return { ...nextState, arrRegion: action.arrRegion };
    }
    return nextState;
}

let testHistory: ReturnType<typeof useHistory> | undefined;

function HistoryCapture(): null {
    const history = useHistory();
    useEffect(() => {
        testHistory = history;
    }, [history]);
    return null;
}

function Launcher({ initialTarget }: { initialTarget: IssueTarget }) {
    const openIssueCreateWindow = useIssueCreateWindow();
    return (
        <>
            <button onClick={() => openIssueCreateWindow({ initialTarget, initialValues })}>Otevřít</button>
            <button onClick={() => openIssueCreateWindow({ initialTarget: 'node', initialValues })}>
                Připomínka k JP
            </button>
        </>
    );
}

/** Another floating window (AI assistant, node search…) sharing the same window layer. */
function OtherWindowLauncher() {
    const { showModal } = useContext(FluentDialogContext);

    function openOtherWindow() {
        showModal<undefined, undefined>({
            createDialog: ({ handleResult }) => (
                <button onClick={() => handleResult(undefined, undefined)}>Zavřít jiné okno</button>
            ),
        });
    }

    return <button onClick={openOtherWindow}>Jiné okno</button>;
}

function protocol(id: number, fundId: number, name: string, open = true) {
    return { id, fundId, name, open, rdUsers: [] as unknown[], wrUsers: [] as unknown[] };
}

const FUND_PROTOCOLS = [
    protocol(11, FUND_ID, 'Protokol A'),
    protocol(12, FUND_ID, 'Protokol B'),
    protocol(13, FUND_ID, 'Uzavřený protokol', false),
];

interface OpenWindowOptions {
    selectedSubNodeId?: number | null;
    /** Protocols of the open fund the server returns. */
    fundProtocols?: ReturnType<typeof protocol>[];
    /** Protocols the user may write to; without it the user is an issue admin of all funds. */
    writableProtocolIds?: number[];
    /** The other fund's protocols never arrive. */
    otherFundProtocolsPending?: boolean;
    /** Another floating window is opened before the comment window. */
    openOtherWindowFirst?: boolean;
}

async function openWindow(
    initialTarget: IssueTarget,
    {
        selectedSubNodeId = 5,
        fundProtocols = FUND_PROTOCOLS,
        writableProtocolIds,
        otherFundProtocolsPending = false,
        openOtherWindowFirst = false,
    }: OpenWindowOptions = {}
) {
    const addedIssues: Record<string, unknown>[] = [];
    server.use(
        http.get('*/issue/issue_types', () => HttpResponse.json([{ id: 2, code: 'IMPORTANT', name: 'Závažná' }])),
        http.get(`*/issue/funds/${FUND_ID}/issue_lists`, () => HttpResponse.json(fundProtocols)),
        http.get(`*/issue/funds/${OTHER_FUND_ID}/issue_lists`, async () => {
            if (otherFundProtocolsPending) {
                await delay('infinite');
            }
            return HttpResponse.json([protocol(21, OTHER_FUND_ID, 'Protokol C')]);
        }),
        http.post('*/issue/issues', async ({ request }) => {
            const issue = (await request.json()) as Record<string, unknown>;
            addedIssues.push(issue);
            return HttpResponse.json({ ...issue, id: 99 });
        })
    );
    const permissionsMap = writableProtocolIds
        ? { FUND_ISSUE_LIST_WR: { issueListIds: writableProtocolIds } }
        : { FUND_ISSUE_ADMIN_ALL: {} };
    const store = createStore(
        testReducer,
        {
            arrRegion: arrRegion({ selectedSubNodeId }),
            userDetail: { id: 1, fetched: true, permissionsMap, userPermissions: [] as unknown[] },
        } as never,
        applyMiddleware(thunk)
    );
    renderWithProviders(
        <UserProvider>
            <FluentDialogProvider hidden={false}>
                <HistoryCapture />
                <OtherWindowLauncher />
                <Launcher initialTarget={initialTarget} />
            </FluentDialogProvider>
        </UserProvider>,
        { store, route: urlFundNode(FUND_ID, undefined, 5) }
    );
    if (openOtherWindowFirst) {
        fireEvent.click(screen.getByRole('button', { name: 'Jiné okno' }));
    }
    fireEvent.click(screen.getByRole('button', { name: 'Otevřít' }));
    await screen.findByRole('option', { name: 'Závažná' });

    const setArrRegion = (options: ArrRegionOptions) =>
        act(() => void store.dispatch({ type: SET_ARR_REGION, arrRegion: arrRegion(options) }));
    const selectSubNode = (nodeId: number) => setArrRegion({ selectedSubNodeId: nodeId });
    const navigate = (path: string) => act(() => testHistory?.push(path));
    /** URL and the store, as the arrangement page does when the user opens another fund. */
    const openOtherFund = () => {
        navigate(urlFundTree(OTHER_FUND_ID));
        setArrRegion({ fundId: OTHER_FUND_ID, selectedSubNodeId: null });
    };
    return { addedIssues, selectSubNode, navigate, openOtherFund, store };
}

function submitIssue(text: string, buttonName = 'Přidat') {
    fireEvent.change(screen.getByLabelText(/Text připomínky/), { target: { value: text } });
    fireEvent.click(screen.getByRole('button', { name: buttonName }));
}

function getOverlay(message: string) {
    const overlay = screen.getByText(message).parentElement;
    if (!overlay) {
        throw new Error(`Overlay with "${message}" not found`);
    }
    return overlay;
}

describe('IssueCreateWindow', () => {
    it('Přidat nechá okno otevřené pro další připomínku ke stejné JP', async () => {
        const { addedIssues } = await openWindow('node');

        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));
        submitIssue('První');
        await waitFor(() => expect(addedIssues).toHaveLength(1));

        expect(screen.getByText('Přidat připomínku - Jednotka popisu')).toBeInTheDocument();
        expect(screen.getByLabelText<HTMLTextAreaElement>(/Text připomínky/).value).toBe('');
        expect(screen.getByRole('button', { name: UNLOCK })).toBeInTheDocument();

        submitIssue('Druhá', 'Přidat a zavřít');
        await waitFor(() => expect(addedIssues).toHaveLength(2));
        expect(addedIssues[1]).toMatchObject({ nodeId: 5, description: 'Druhá' });
        await waitFor(() => expect(screen.queryByText('Přidat připomínku - Jednotka popisu')).not.toBeInTheDocument());
    });

    it('k JP sleduje vybranou jednotku popisu, po přidání a zavření se okno zavře', async () => {
        const { addedIssues, selectSubNode } = await openWindow('node');

        expect(screen.getByText('Přidat připomínku - Jednotka popisu')).toBeInTheDocument();
        expect(screen.getByText('1 2')).toBeInTheDocument();
        expect(screen.getByText('Listina')).toBeInTheDocument();
        expect(document.querySelector('[style*="-resize"]')).toBeNull();

        selectSubNode(6);
        expect(await screen.findByText('Kronika')).toBeInTheDocument();

        submitIssue('Chybí datace', 'Přidat a zavřít');

        await waitFor(() => expect(screen.queryByText('Přidat připomínku - Jednotka popisu')).not.toBeInTheDocument());
        expect(addedIssues).toEqual([
            expect.objectContaining({ nodeId: 6, issueListId: 11, issueTypeId: 2, description: 'Chybí datace' }),
        ]);
    });

    it('zamčená JP zůstane i po výběru jiné jednotky popisu, odemčení vrátí sledování', async () => {
        const { addedIssues, selectSubNode } = await openWindow('node');

        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));
        selectSubNode(6);

        expect(screen.getByText('Listina')).toBeInTheDocument();
        expect(screen.queryByText('Kronika')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: UNLOCK }));
        expect(await screen.findByText('Kronika')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));
        selectSubNode(5);
        submitIssue('Zamčeno');

        await waitFor(() => expect(addedIssues).toHaveLength(1));
        expect(addedIssues[0]).toMatchObject({ nodeId: 6 });
    });

    it('odkaz s označením a názvem JP přejde na zamčenou jednotku popisu', async () => {
        const { selectSubNode, store } = await openWindow('node');

        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));
        selectSubNode(6);
        fireEvent.click(screen.getByText('Listina'));

        expect(store.getState().router.navigateTo).toBe(urlFundNode(FUND_ID, undefined, 5));
    });

    it('odkaz na JP zamčenou v uzavřené verzi AS otevře tuto verzi', async () => {
        const { navigate, selectSubNode, store } = await openWindow('node');
        navigate(urlFundNode(FUND_ID, 7, 5));

        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));
        selectSubNode(6);
        fireEvent.click(screen.getByText('Listina'));

        expect(store.getState().router.navigateTo).toBe(urlFundNode(FUND_ID, 7, 5));
    });

    it('připomínka k AS se odešle bez JP, přepínač mění cíl', async () => {
        const { addedIssues } = await openWindow('fund');

        expect(screen.getByText('Přidat připomínku - Archivní soubor')).toBeInTheDocument();
        expect(screen.queryByText('Listina')).not.toBeInTheDocument();

        const fundButton = screen.getByRole('button', { name: 'Archivní soubor' });
        const nodeButton = screen.getByRole('button', { name: 'Jednotka popisu' });
        expect(fundButton).toHaveAttribute('aria-pressed', 'true');

        fireEvent.click(nodeButton);
        expect(nodeButton).toHaveAttribute('aria-pressed', 'true');
        expect(fundButton).toHaveAttribute('aria-pressed', 'false');
        expect(screen.getByText('Listina')).toBeInTheDocument();
        fireEvent.click(fundButton);

        submitIssue('K celému AS');

        await waitFor(() => expect(addedIssues).toHaveLength(1));
        expect(addedIssues[0]).not.toHaveProperty('nodeId');
    });

    it('bez vybrané jednotky popisu nejde připomínku k JP přidat ani zamknout', async () => {
        await openWindow('node', { selectedSubNodeId: null });

        expect(screen.getByText('Není vybrána žádná jednotka popisu')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Přidat' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' })).toBeDisabled();
    });

    it('bez otevřeného AS (podle URL, ne podle AS ve store) překryje formulář, rozepsané zůstane', async () => {
        const { navigate } = await openWindow('node');
        const textArea = screen.getByLabelText<HTMLTextAreaElement>(/Text připomínky/);
        fireEvent.change(textArea, { target: { value: 'Rozepsáno' } });

        // the fund's detail in the fund list; the store still has the fund loaded
        navigate(urlFund(FUND_ID));
        expect(await screen.findByText('Není otevřen žádný archivní soubor')).toBeInTheDocument();
        expect(screen.getByRole('status')).toHaveTextContent('Není otevřen žádný archivní soubor');
        expect(textArea.closest('[inert]')).not.toBeNull();

        navigate(urlFundNode(FUND_ID, 7, 5));
        await waitFor(() => expect(screen.queryByText('Není otevřen žádný archivní soubor')).not.toBeInTheDocument());
        expect(textArea.closest('[inert]')).toBeNull();
        expect(textArea.value).toBe('Rozepsáno');
    });

    it('nezamčená připomínka sleduje i jiný otevřený AS a přepne se na jeho protokol', async () => {
        const { addedIssues, openOtherFund } = await openWindow('fund');

        openOtherFund();

        await waitFor(() => expect(screen.getByLabelText<HTMLSelectElement>(/Protokol/).value).toBe('21'));
        expect(screen.queryByText(LOCKED_ELSEWHERE)).not.toBeInTheDocument();

        submitIssue('V jiném AS');

        await waitFor(() => expect(addedIssues).toHaveLength(1));
        expect(addedIssues[0]).toMatchObject({ issueListId: 21 });
    });

    it('zamčená JP z jiného AS: hláška s odkazem na JP, odemčením začne sledovat otevřený AS', async () => {
        const { openOtherFund, store } = await openWindow('node');

        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));
        openOtherFund();

        const overlay = getOverlay(LOCKED_ELSEWHERE);
        expect(within(overlay).getByText('1 2')).toBeInTheDocument();
        fireEvent.click(within(overlay).getByText('Listina'));
        expect(store.getState().router.navigateTo).toBe(urlFundNode(FUND_ID, undefined, 5));

        fireEvent.click(within(overlay).getByRole('button', { name: UNLOCK }));

        expect(screen.queryByText(LOCKED_ELSEWHERE)).not.toBeInTheDocument();
        expect(screen.getByText('Není vybrána žádná jednotka popisu')).toBeInTheDocument();
    });

    it('bez oprávnění přidávat připomínky v AS překryje formulář hláškou', async () => {
        await openWindow('node', { writableProtocolIds: [] });

        expect(await screen.findByText(NO_PERMISSION)).toBeInTheDocument();
        expect(screen.getByLabelText(/Text připomínky/).closest('[inert]')).not.toBeNull();
    });

    it('nabídne jen otevřené protokoly, do kterých smí uživatel zapisovat', async () => {
        const { addedIssues } = await openWindow('fund', { writableProtocolIds: [12, 13] });

        await waitFor(() => expect(screen.getByLabelText<HTMLSelectElement>(/Protokol/).value).toBe('12'));
        expect(screen.queryByRole('option', { name: 'Protokol A' })).not.toBeInTheDocument();
        expect(screen.queryByRole('option', { name: 'Uzavřený protokol' })).not.toBeInTheDocument();
        expect(screen.queryByText(NO_PERMISSION)).not.toBeInTheDocument();

        submitIssue('Do zapisovatelného protokolu');

        await waitFor(() => expect(addedIssues).toHaveLength(1));
        expect(addedIssues[0]).toMatchObject({ issueListId: 12 });
    });

    it('zápis jen do uzavřeného protokolu AS překryje formulář hláškou o oprávnění', async () => {
        await openWindow('fund', { writableProtocolIds: [13, 21] });

        expect(await screen.findByText(NO_PERMISSION)).toBeInTheDocument();
    });

    it('AS bez protokolů překryje formulář hláškou', async () => {
        await openWindow('fund', { fundProtocols: [] });

        expect(await screen.findByText(NO_OPEN_PROTOCOL)).toBeInTheDocument();
        expect(screen.getByLabelText(/Text připomínky/).closest('[inert]')).not.toBeNull();
    });

    it('AS jen s uzavřeným protokolem překryje formulář hláškou i správci připomínek', async () => {
        await openWindow('fund', { fundProtocols: [protocol(13, FUND_ID, 'Uzavřený protokol', false)] });

        expect(await screen.findByText(NO_OPEN_PROTOCOL)).toBeInTheDocument();
        expect(screen.queryByText(NO_PERMISSION)).not.toBeInTheDocument();
    });

    it('po přechodu na jiný AS jen v URL (store ho ještě nepřepnul) nabízí protokoly AS z URL', async () => {
        const { addedIssues, navigate } = await openWindow('fund');

        navigate(urlFundTree(OTHER_FUND_ID));

        await waitFor(() => expect(screen.getByLabelText<HTMLSelectElement>(/Protokol/).value).toBe('21'));
        submitIssue('V jiném AS');

        await waitFor(() => expect(addedIssues).toHaveLength(1));
        expect(addedIssues[0]).toMatchObject({ issueListId: 21 });
    });

    it('dokud nejsou načtené protokoly otevřeného AS, nejde připomínku přidat', async () => {
        const { navigate } = await openWindow('fund', { otherFundProtocolsPending: true });

        navigate(urlFundTree(OTHER_FUND_ID));

        await waitFor(() => expect(screen.getByRole('button', { name: 'Přidat' })).toBeDisabled());
        expect(screen.getByRole('button', { name: 'Přidat a zavřít' })).toBeDisabled();
    });

    it('zavření dříve otevřeného jiného okna nesmaže rozepsanou připomínku ani zámek', async () => {
        await openWindow('node', { openOtherWindowFirst: true });
        fireEvent.change(screen.getByLabelText(/Text připomínky/), { target: { value: 'Rozepsáno' } });
        fireEvent.click(screen.getByRole('button', { name: 'Zamknout na této jednotce popisu' }));

        fireEvent.click(screen.getByRole('button', { name: 'Zavřít jiné okno' }));

        expect(screen.getByLabelText<HTMLTextAreaElement>(/Text připomínky/).value).toBe('Rozepsáno');
        expect(screen.getByRole('button', { name: UNLOCK })).toBeInTheDocument();
    });

    it('opakované přidání k JP přepne otevřené okno na JP a rozepsaný text nechá', async () => {
        await openWindow('fund');
        fireEvent.change(screen.getByLabelText(/Text připomínky/), { target: { value: 'Rozepsáno' } });

        fireEvent.click(screen.getByRole('button', { name: 'Připomínka k JP' }));

        expect(screen.getAllByText('Přidat připomínku - Jednotka popisu')).toHaveLength(1);
        expect(screen.getByRole('button', { name: 'Jednotka popisu' })).toHaveAttribute('aria-pressed', 'true');
        expect(screen.getByLabelText<HTMLTextAreaElement>(/Text připomínky/).value).toBe('Rozepsáno');
    });

    it('druhé otevření nechá jediné okno', async () => {
        await openWindow('node');

        fireEvent.click(screen.getByRole('button', { name: 'Otevřít' }));

        expect(screen.getAllByText('Přidat připomínku - Jednotka popisu')).toHaveLength(1);
    });

    it('okno se zavírá křížkem v záhlaví, formulář nemá tlačítko Zrušit', async () => {
        await openWindow('node');

        expect(screen.queryByRole('button', { name: 'Zrušit' })).not.toBeInTheDocument();
    });
});
