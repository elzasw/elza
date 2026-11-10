import { describe, expect, it, vi } from 'vitest';
import { HttpResponse, http } from 'msw';
import userEvent from '@testing-library/user-event';
import { ApSearchArea } from 'elza-api';

import { server } from 'test/mocks/server';
import { renderWithProviders, screen, waitFor } from 'test/test-utils';
import RelationFilterModal from './RelationFilterModal';

/**
 * Relation filter modal (Archivní entity → Rozšířený filtr → Vztahy).
 *
 * The form fields have no associated label, so they are found through the label text and its parent.
 */

interface ItemSpecFixture {
    id: number;
    code: string;
    name: string;
}

interface ItemTypeFixture {
    id: number;
    code: string;
    name: string;
    dataTypeId: number;
    useSpecification: boolean;
    descItemSpecs: ItemSpecFixture[];
}

const RULE_SET_ID = 7;
const DATA_TYPE_RECORD_REF = { id: 1, code: 'RECORD_REF' };

const AUTHOR_SPECS: ItemSpecFixture[] = [
    { id: 101, code: 'SPEC_MAIN', name: 'Hlavní autor' },
    { id: 102, code: 'SPEC_CO', name: 'Spoluautor' },
];

const NO_SPECS: ItemSpecFixture[] = [];

const ITEM_TYPES: ItemTypeFixture[] = [
    { id: 10, code: 'REL_AUTHOR', name: 'Autor', dataTypeId: 1, useSpecification: true, descItemSpecs: AUTHOR_SPECS },
    { id: 11, code: 'REL_PLACE', name: 'Místo', dataTypeId: 1, useSpecification: false, descItemSpecs: NO_SPECS },
];

const PRELOADED_STATE: Record<string, unknown> = {
    refTables: {
        descItemTypes: {
            fetched: true,
            items: ITEM_TYPES,
            itemsMap: Object.fromEntries(ITEM_TYPES.map((itemType) => [itemType.id, itemType])),
        },
        rulDataTypes: {
            fetched: true,
            items: [DATA_TYPE_RECORD_REF],
            itemsMap: { 1: DATA_TYPE_RECORD_REF },
        },
        apTypes: {
            fetched: true,
            itemsMap: { 5: { id: 5, name: 'Osoba' } },
        },
    },
};

interface RelSearchRequest {
    params: URLSearchParams;
    body: unknown;
}

function mockServer() {
    const relSearches: RelSearchRequest[] = [];
    server.use(
        http.get(`/api/rule/itemTypeCodes/${RULE_SET_ID}`, () => HttpResponse.json(ITEM_TYPES.map(({ code }) => code))),
        http.post('/api/registry/search/rel', async ({ request }) => {
            relSearches.push({ params: new URL(request.url).searchParams, body: await request.json() });
            return HttpResponse.json({
                total: 1,
                data: [{ id: 42, name: 'Novák Jan', aeTypeId: 5, description: 'Spisovatel' }],
            });
        })
    );
    return relSearches;
}

function renderModal() {
    const onSubmit = vi.fn();
    renderWithProviders(
        <RelationFilterModal
            initialValues={{
                area: ApSearchArea.AllNames,
                onlyMainPart: false,
                scopeId: 3,
                itemType: { id: null, name: 'Vše' },
            }}
            onSubmit={onSubmit}
            onClose={vi.fn()}
            rulSetsIds={[RULE_SET_ID]}
        />,
        { preloadedState: PRELOADED_STATE }
    );
    return { onSubmit };
}

/** Input in the same wrapper as the label with the given text. */
function inputByLabel(labelText: string): HTMLInputElement {
    const input = screen.getByText(labelText).parentElement?.querySelector<HTMLInputElement>('input');
    if (!input) {
        throw new Error(`No input next to the label "${labelText}"`);
    }
    return input;
}

type User = ReturnType<typeof userEvent.setup>;

async function pickItem(user: User, labelText: string, searchText: string, itemName: string) {
    const input = inputByLabel(labelText);
    await user.clear(input);
    await user.type(input, searchText);
    // the entity search is debounced by 1 s
    await user.click(await screen.findByText(itemName, {}, { timeout: 3000 }));
}

describe('RelationFilterModal', () => {
    it('drops the specification when the relation type changes', async () => {
        const relSearches = mockServer();
        const { onSubmit } = renderModal();
        const user = userEvent.setup();

        const itemTypeInput = inputByLabel('Typ vztahu');
        await user.clear(itemTypeInput);
        await user.type(itemTypeInput, 'Aut');
        await user.click(await screen.findByText('Autor'));
        await user.type(inputByLabel('Specifikace'), 'Hlav');
        await user.click(await screen.findByText('Hlavní autor'));

        await user.clear(itemTypeInput);
        await user.type(itemTypeInput, 'Mís');
        await user.click(await screen.findByText('Místo'));
        expect(screen.queryByText('Specifikace')).not.toBeInTheDocument();

        await user.type(inputByLabel('Archivní entita'), 'Nov');
        await user.click(await screen.findByText('Novák Jan', {}, { timeout: 3000 }));

        await waitFor(() => expect(relSearches).toHaveLength(1));
        const [search] = relSearches;
        expect(search.params.get('itemTypeId')).toBe('11');
        expect(search.params.has('itemSpecId')).toBe(false);
        expect(search.body).toMatchObject({ relFilters: [{ relTypeId: 11 }] });

        await user.click(screen.getByRole('button', { name: 'Použít' }));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
        const [values] = onSubmit.mock.calls[0];
        expect(values.itemType).toMatchObject({ id: 11, name: 'Místo' });
        expect(values.itemSpec).toBeUndefined();
    });

    it('drops the related entity when the relation type or the specification changes', async () => {
        mockServer();
        renderModal();
        const user = userEvent.setup();

        await pickItem(user, 'Typ vztahu', 'Aut', 'Autor');
        await pickItem(user, 'Specifikace', 'Hlav', 'Hlavní autor');
        await pickItem(user, 'Archivní entita', 'Nov', 'Novák Jan');
        expect(inputByLabel('Archivní entita').value).toBe('Novák Jan');

        await pickItem(user, 'Specifikace', 'Spol', 'Spoluautor');
        expect(inputByLabel('Archivní entita').value).toBe('');

        await pickItem(user, 'Archivní entita', 'Nov', 'Novák Jan');
        await pickItem(user, 'Typ vztahu', 'Mís', 'Místo');
        expect(inputByLabel('Archivní entita').value).toBe('');
    });
});
