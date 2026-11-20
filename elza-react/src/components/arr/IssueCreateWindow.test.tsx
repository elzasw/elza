import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';

import { fireEvent, renderWithProviders, screen, waitFor } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { FluentDialogProvider } from 'components/shared/dialog/FluentModalDialog';
import { IssueFormValues } from 'components/form/IssueForm';
import { useIssueCreateWindow } from './useIssueCreateWindow';

/**
 * Přidání připomínky lektorování v nemodálním okně: v záhlaví je referenční označení JP,
 * velikost okna nejde měnit, pro jednu JP je otevřené nejvýš jedno okno a po uložení se okno zavře.
 */

const FUND_ID = 3;
const initialValues: IssueFormValues = { issueListId: 11, issueTypeId: 2 };

function Launcher() {
    const openIssueCreateWindow = useIssueCreateWindow();
    return (
        <>
            <button onClick={() => openIssueCreateWindow({ nodeId: 5, referenceMark: ['1', '2'], initialValues })}>
                JP 5
            </button>
            <button onClick={() => openIssueCreateWindow({ nodeId: 6, referenceMark: ['1', '3'], initialValues })}>
                JP 6
            </button>
        </>
    );
}

function renderLauncher() {
    const addedIssues: unknown[] = [];
    server.use(
        http.get('*/issue/issue_types', () => HttpResponse.json([{ id: 2, code: 'IMPORTANT', name: 'Závažná' }])),
        http.get(`*/issue/funds/${FUND_ID}/issue_lists`, () =>
            HttpResponse.json([{ id: 11, fundId: FUND_ID, name: 'Protokol A', open: true, rdUsers: [], wrUsers: [] }])
        ),
        http.post('*/issue/issues', async ({ request }) => {
            const issue = (await request.json()) as Record<string, unknown>;
            addedIssues.push(issue);
            return HttpResponse.json({ ...issue, id: 99 });
        })
    );
    renderWithProviders(
        <FluentDialogProvider hidden={false}>
            <Launcher />
        </FluentDialogProvider>,
        { preloadedState: { arrRegion: { activeIndex: 0, funds: [{ id: FUND_ID }] } } }
    );
    return { addedIssues };
}

const NODE_5_TITLE = 'Přidat připomínku - Jednotka popisu 1 2';
const NODE_6_TITLE = 'Přidat připomínku - Jednotka popisu 1 3';

describe('IssueCreateWindow', () => {
    it('v záhlaví ukáže referenční označení, připomínku přidá k JP a okno zavře', async () => {
        const { addedIssues } = renderLauncher();

        fireEvent.click(screen.getByRole('button', { name: 'JP 5' }));
        expect(await screen.findByText(NODE_5_TITLE)).toBeInTheDocument();
        await screen.findByRole('option', { name: 'Závažná' });
        expect(document.querySelector('[style*="-resize"]')).toBeNull();

        fireEvent.change(screen.getByLabelText(/Text připomínky/), { target: { value: 'Chybí datace' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat' }));

        await waitFor(() => expect(screen.queryByText(NODE_5_TITLE)).not.toBeInTheDocument());
        expect(addedIssues).toEqual([
            expect.objectContaining({ nodeId: 5, issueListId: 11, issueTypeId: 2, description: 'Chybí datace' }),
        ]);
    });

    it('pro stejnou JP otevře jen jedno okno, pro jinou JP další', async () => {
        renderLauncher();

        fireEvent.click(screen.getByRole('button', { name: 'JP 5' }));
        await screen.findByText(NODE_5_TITLE);
        fireEvent.click(screen.getByRole('button', { name: 'JP 5' }));
        fireEvent.click(screen.getByRole('button', { name: 'JP 6' }));

        expect(await screen.findByText(NODE_6_TITLE)).toBeInTheDocument();
        expect(screen.getAllByText(NODE_5_TITLE)).toHaveLength(1);
    });
});
