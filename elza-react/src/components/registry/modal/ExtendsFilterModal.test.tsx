import { describe, expect, it, vi } from 'vitest';
import { HttpResponse, http } from 'msw';
import userEvent from '@testing-library/user-event';
import { ApSearchArea } from 'elza-api';

import { server } from 'test/mocks/server';
import { renderWithProviders, screen, waitFor } from 'test/test-utils';
import ExtendsFilterModal from './ExtendsFilterModal';

/**
 * Item filter modal (Archivní entity → Rozšířený filtr → Prvky popisu).
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
const DATA_TYPE_STRING = { id: 1, code: 'STRING' };
const DATA_TYPE_RECORD_REF = { id: 2, code: 'RECORD_REF' };

const NAME_SPECS: ItemSpecFixture[] = [
    { id: 201, code: 'SPEC_BIRTH', name: 'Rodné' },
    { id: 202, code: 'SPEC_PSEUDONYM', name: 'Pseudonym' },
];

const NO_SPECS: ItemSpecFixture[] = [];

const ITEM_TYPES: ItemTypeFixture[] = [
    { id: 20, code: 'NM_MAIN', name: 'Jméno', dataTypeId: 1, useSpecification: true, descItemSpecs: NAME_SPECS },
    { id: 21, code: 'NOTE', name: 'Poznámka', dataTypeId: 1, useSpecification: false, descItemSpecs: NO_SPECS },
    { id: 22, code: 'REL_AUTHOR', name: 'Autor', dataTypeId: 2, useSpecification: false, descItemSpecs: NO_SPECS },
    { id: 23, code: 'REL_PLACE', name: 'Místo', dataTypeId: 2, useSpecification: false, descItemSpecs: NO_SPECS },
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
            items: [DATA_TYPE_STRING, DATA_TYPE_RECORD_REF],
            itemsMap: { 1: DATA_TYPE_STRING, 2: DATA_TYPE_RECORD_REF },
        },
        partTypes: {
            fetched: true,
            items: [],
        },
        apTypes: {
            fetched: true,
            itemsMap: { 5: { id: 5, name: 'Osoba' } },
        },
    },
};

function mockServer() {
    server.use(
        http.get(`/api/rule/itemTypeCodes/${RULE_SET_ID}`, () => HttpResponse.json(ITEM_TYPES.map(({ code }) => code))),
        http.post('/api/registry/search/rel', () =>
            HttpResponse.json({
                total: 1,
                data: [{ id: 42, name: 'Novák Jan', aeTypeId: 5, description: 'Spisovatel' }],
            })
        )
    );
}

function renderModal() {
    const onSubmit = vi.fn();
    renderWithProviders(
        <ExtendsFilterModal
            initialValues={{
                onlyMainPart: true,
                area: ApSearchArea.AllNames,
                scopeId: 3,
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

describe('ExtendsFilterModal', () => {
    it('drops the specification and the value when the item type changes', async () => {
        mockServer();
        const { onSubmit } = renderModal();
        const user = userEvent.setup();

        await pickItem(user, 'Typ prvku popisu', 'Jmé', 'Jméno');
        await pickItem(user, 'Specifikace', 'Rod', 'Rodné');
        await user.type(inputByLabel('Hodnota'), 'Novák');

        await pickItem(user, 'Typ prvku popisu', 'Pozn', 'Poznámka');
        expect(screen.queryByText('Specifikace')).not.toBeInTheDocument();
        expect(inputByLabel('Hodnota').value).toBe('');

        await user.type(inputByLabel('Hodnota'), 'spisovatel');
        await user.click(screen.getByRole('button', { name: 'Použít' }));

        await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
        const [values] = onSubmit.mock.calls[0];
        expect(values.itemType).toMatchObject({ id: 21, name: 'Poznámka' });
        expect(values.itemSpec).toBeUndefined();
        expect(values.value).toBe('spisovatel');
    });

    it('drops the related entity when the item type changes', async () => {
        mockServer();
        renderModal();
        const user = userEvent.setup();

        await pickItem(user, 'Typ prvku popisu', 'Aut', 'Autor');
        await pickItem(user, 'Archivní entita', 'Nov', 'Novák Jan');
        expect(inputByLabel('Archivní entita').value).toBe('Novák Jan');

        await pickItem(user, 'Typ prvku popisu', 'Mís', 'Místo');
        expect(inputByLabel('Archivní entita').value).toBe('');
    });
});
