import { DataString, DataText, DataType } from 'elza-api';
import { isDataString, isDataText } from '../templates/types';
import { ClipboardItem } from './itemClipboard';

export interface ItemTypeInfo {
    /** Data type code of the item type, e.g. `STRING`. */
    dataType?: string;
    useSpecification: boolean;
}

export type GetItemTypeInfo = (itemTypeId: number) => ItemTypeInfo | undefined;

const remappableDataTypes: string[] = [DataType.String, DataType.Text];

function isRemappableType(info: ItemTypeInfo | undefined) {
    return info != undefined && !info.useSpecification && remappableDataTypes.includes(info.dataType ?? '');
}

/** Same as the backend `StringNormalize.normalizeString`: control characters (line breaks too) become spaces. */
export function normalizeString(value: string) {
    const printable = Array.from(value, (character) => (character.charCodeAt(0) > 0x1f ? character : ' ')).join('');
    return printable.replace(/ +/g, ' ').trim();
}

function remapItem(item: ClipboardItem, targetTypeId: number, targetDataType: string): ClipboardItem {
    const data = item.data;
    if (data && isDataText(data) && targetDataType === DataType.String) {
        const stringData: DataString = { dataType: DataType.String, stringValue: normalizeString(data.textValue ?? '') };
        return { ...item, itemTypeId: targetTypeId, data: stringData };
    }
    if (data && isDataString(data) && targetDataType === DataType.Text) {
        const textData: DataText = { dataType: DataType.Text, textValue: data.stringValue };
        return { ...item, itemTypeId: targetTypeId, data: textData };
    }
    return { ...item, itemTypeId: targetTypeId };
}

/**
 * Clipboard items to paste into one DescItemType: the items of that type when there are any,
 * otherwise the STRING/TEXT values of another type remapped to it. Remapping needs neither type to
 * use a specification and exactly one such source type in the clipboard; undefined items are not remapped.
 */
export function selectItemsForType(
    items: ClipboardItem[],
    targetTypeId: number,
    getItemTypeInfo: GetItemTypeInfo,
): ClipboardItem[] {
    const sameTypeItems = items.filter(({ itemTypeId }) => itemTypeId === targetTypeId);
    if (sameTypeItems.length > 0) {
        return sameTypeItems;
    }

    const targetInfo = getItemTypeInfo(targetTypeId);
    if (!isRemappableType(targetInfo) || !targetInfo?.dataType) {
        return [];
    }

    const remappableItems = items.filter(
        (item) =>
            item.itemTypeId != undefined &&
            !item.undefined &&
            item.data != undefined &&
            remappableDataTypes.includes(item.data.dataType) &&
            isRemappableType(getItemTypeInfo(item.itemTypeId)),
    );
    const sourceTypeIds = new Set(remappableItems.map(({ itemTypeId }) => itemTypeId));
    if (sourceTypeIds.size !== 1) {
        return [];
    }

    const targetDataType = targetInfo.dataType;
    return remappableItems.map((item) => remapItem(item, targetTypeId, targetDataType));
}
