import { describe, it, expect, vi } from 'vitest';

import { createTestStore, renderWithProviders, screen, fireEvent } from 'test/test-utils';
import type { TestStore } from 'test/test-utils';
import * as types from 'actions/constants/ActionTypes';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import type { RulDataTypeVO } from 'api/RulDataTypeVO';
import type { RulDescItemSpecExtVO } from 'api/RulDescItemSpecExtVO';
import type { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import type { BulkAppendValue } from './BulkAppendValues';
import FundBulkModificationsForm, { OperationType } from './FundBulkModificationsForm';

/**
 * Hromadná úprava v tabulkovém zobrazení: operace Přidat bere seznam hodnot
 * (řádky se specifikací a hodnotou) a posílá je jako typované ItemData.
 *
 * Texty jsou na react-intl, takže se vykreslí jako české defaultMessage.
 * Pole se hledají podle atributu name.
 */

vi.mock('actions/refTables/descItemTypes.jsx', () => ({
    descItemTypesFetchIfNeeded: () => (): void => undefined,
}));

const spec = (id: number, name: string) => ({ id, name }) as RulDescItemSpecExtVO;

const itemType = (name: string): RulDescItemTypeExtVO =>
    ({
        id: 5,
        name,
        shortcut: name,
        useSpecification: true,
        descItemSpecs: [spec(11, 'čeština'), spec(12, 'angličtina')],
    }) as RulDescItemTypeExtVO;

const VERSION_ID = 7;
const STRUCTURE_TYPE_ID = 3;

const packetItemType = {
    id: 6,
    name: 'Ukládací jednotka',
    shortcut: 'Ukládací jednotka',
    useSpecification: false,
    descItemSpecs: [],
    structureTypeId: STRUCTURE_TYPE_ID,
} as RulDescItemTypeExtVO;

const dataType = (code: RulDataTypeCodeEnum) => ({ id: 1, code, name: code }) as RulDataTypeVO;

interface RenderOptions {
    refType?: RulDescItemTypeExtVO;
    appendValues?: BulkAppendValue[];
    store?: TestStore;
}

const renderForm = (
    code: RulDataTypeCodeEnum,
    operationType: OperationType,
    onSubmitForm = vi.fn(),
    { refType = itemType('Jazyk'), appendValues = [{}], store }: RenderOptions = {}
) =>
    renderWithProviders(
        <FundBulkModificationsForm
            initialValues={{
                itemsArea: 'page',
                specs: { type: 'unselected', ids: [] },
                appendValues,
                operationType,
            }}
            dataType={dataType(code)}
            refType={refType}
            versionId={VERSION_ID}
            allItemsCount={3}
            checkedItemsCount={0}
            onClose={vi.fn()}
            onSubmitForm={onSubmitForm}
        />,
        { store }
    );

const storeWithPacketType = (anonymous: boolean) => {
    const store = createTestStore();
    store.dispatch({
        type: types.REF_STRUCTURE_TYPES_RECEIVE,
        versionId: VERSION_ID,
        data: [{ id: STRUCTURE_TYPE_ID, name: 'Obal', code: 'SRD_PACKET', anonymous }],
    });
    return store;
};

const field = (container: HTMLElement, tag: 'select' | 'input', name: string) => {
    const element = container.querySelector<HTMLSelectElement | HTMLInputElement>(`${tag}[name="${name}"]`);
    if (!element) {
        throw new Error(`Field ${name} not found`);
    }
    return element;
};

describe('FundBulkModificationsForm - přidání více hodnot', () => {
    it('odešle dvě hodnoty výčtu jako ItemData se specifikací', async () => {
        const onSubmitForm = vi.fn().mockResolvedValue(undefined);
        const { container } = renderForm(RulDataTypeCodeEnum.ENUM, OperationType.APPEND, onSubmitForm);

        fireEvent.change(field(container, 'select', 'appendValues[0].specId'), { target: { value: '11' } });
        fireEvent.click(screen.getByRole('button', { name: 'Přidat hodnotu' }));
        fireEvent.change(field(container, 'select', 'appendValues[1].specId'), { target: { value: '12' } });
        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));

        await vi.waitFor(() => expect(onSubmitForm).toHaveBeenCalledTimes(1));
        expect(onSubmitForm.mock.calls[0][0]).toMatchObject({
            operationType: OperationType.APPEND,
            appendItemValues: [
                { itemSpecId: 11, data: { dataType: 'ENUM' } },
                { itemSpecId: 12, data: { dataType: 'ENUM' } },
            ],
        });
    });

    it('převede textovou hodnotu na ItemData podle datového typu', async () => {
        const onSubmitForm = vi.fn().mockResolvedValue(undefined);
        const { container } = renderForm(RulDataTypeCodeEnum.STRING, OperationType.APPEND, onSubmitForm);

        fireEvent.change(field(container, 'select', 'appendValues[0].specId'), { target: { value: '12' } });
        fireEvent.change(field(container, 'input', 'appendValues[0].value'), { target: { value: 'hodnota' } });
        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));

        await vi.waitFor(() => expect(onSubmitForm).toHaveBeenCalledTimes(1));
        expect(onSubmitForm.mock.calls[0][0]).toMatchObject({
            appendItemValues: [{ itemSpecId: 12, data: { dataType: 'STRING', stringValue: 'hodnota' } }],
        });
    });

    it('desetinné číslo přijme s čárkou, text odmítne', async () => {
        const onSubmitForm = vi.fn().mockResolvedValue(undefined);
        const { container } = renderForm(RulDataTypeCodeEnum.DECIMAL, OperationType.APPEND, onSubmitForm);
        fireEvent.change(field(container, 'select', 'appendValues[0].specId'), { target: { value: '11' } });

        fireEvent.change(field(container, 'input', 'appendValues[0].value'), { target: { value: 'abc' } });
        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));
        expect(await screen.findByText('Nejedná se o číslo')).toBeInTheDocument();
        expect(onSubmitForm).not.toHaveBeenCalled();

        fireEvent.change(field(container, 'input', 'appendValues[0].value'), { target: { value: '1,5' } });
        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));
        await vi.waitFor(() => expect(onSubmitForm).toHaveBeenCalledTimes(1));
        expect(onSubmitForm.mock.calls[0][0]).toMatchObject({
            appendItemValues: [{ itemSpecId: 11, data: { dataType: 'DECIMAL', value: 1.5 } }],
        });
    });

    it('nevyplněný řádek ukáže chybu a formulář neodešle', async () => {
        const onSubmitForm = vi.fn();
        renderForm(RulDataTypeCodeEnum.ENUM, OperationType.APPEND, onSubmitForm);

        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));

        expect(await screen.findByText('Pole je povinné')).toBeInTheDocument();
        expect(onSubmitForm).not.toHaveBeenCalled();
    });

    it('jediný řádek nejde odebrat, přidaný ano', () => {
        renderForm(RulDataTypeCodeEnum.ENUM, OperationType.APPEND);

        expect(screen.getByRole('button', { name: 'Odebrat hodnotu' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Přidat hodnotu' }));
        const removeButtons = screen.getAllByRole('button', { name: 'Odebrat hodnotu' });
        expect(removeButtons).toHaveLength(2);
        removeButtons.forEach((button) => expect(button).toBeEnabled());
    });

    it('odešle strukturovaný objekt jako DataStructureRef, prázdný řádek odmítne', async () => {
        const onSubmitForm = vi.fn().mockResolvedValue(undefined);
        renderForm(RulDataTypeCodeEnum.STRUCTURED, OperationType.APPEND, onSubmitForm, {
            refType: packetItemType,
            appendValues: [{ rowId: 0, value: { id: 70, name: 'Krabice 1' } }, { rowId: 1 }],
        });

        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));
        expect(await screen.findByText('Pole je povinné')).toBeInTheDocument();
        expect(onSubmitForm).not.toHaveBeenCalled();

        fireEvent.click(screen.getAllByRole('button', { name: 'Odebrat hodnotu' })[1]);
        fireEvent.click(screen.getByRole('button', { name: 'Uložit' }));
        await vi.waitFor(() => expect(onSubmitForm).toHaveBeenCalledTimes(1));
        expect(onSubmitForm.mock.calls[0][0]).toMatchObject({
            appendItemValues: [{ data: { dataType: 'STRUCTURED', structuredObjectId: 70 } }],
        });
    });

    it('přidání strukturovaného prvku nabízí jen pro neanonymní typ', () => {
        const appendOption = { name: 'Přidat obsah prvku popisu' };
        const { unmount } = renderForm(RulDataTypeCodeEnum.STRUCTURED, OperationType.DELETE, vi.fn(), {
            refType: packetItemType,
            store: storeWithPacketType(false),
        });
        expect(screen.getByRole('option', appendOption)).toBeInTheDocument();
        unmount();

        renderForm(RulDataTypeCodeEnum.STRUCTURED, OperationType.DELETE, vi.fn(), {
            refType: packetItemType,
            store: storeWithPacketType(true),
        });
        expect(screen.queryByRole('option', appendOption)).not.toBeInTheDocument();
    });

    it('seznam specifikací k filtrování nabízí smazání, přidání ne', () => {
        const { unmount } = renderForm(RulDataTypeCodeEnum.ENUM, OperationType.DELETE);
        expect(screen.getByText('Aplikovat na hodnoty')).toBeInTheDocument();
        unmount();

        renderForm(RulDataTypeCodeEnum.ENUM, OperationType.APPEND);
        expect(screen.queryByText('Aplikovat na hodnoty')).not.toBeInTheDocument();
    });
});
