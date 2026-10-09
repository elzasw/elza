import { RefTablesState } from 'typings/store';
import { ApItemVO } from '../api/ApItemVO';
import { ApViewSettingRule } from '../api/ApViewSettings';
import { RulPartTypeVO } from '../api/RulPartTypeVO';
import { findViewItemType } from './ItemInfo';
import { RevisionItem } from '../components/registry/revision';

/**
 * Order of two item types in a part: the position from the view settings of the rule set first
 * (types with a position before those without), then the global view order of the item type.
 */
export function compareItemTypes(
    aTypeId: number,
    bTypeId: number,
    partTypeId: number,
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): number {
    const part: RulPartTypeVO | undefined = refTables.partTypes.itemsMap[partTypeId];
    const descItemTypesMap = refTables.descItemTypes.itemsMap;

    const aInfo = descItemTypesMap[aTypeId];
    const bInfo = descItemTypesMap[bTypeId];

    if (!aInfo || !bInfo) {
        return aInfo ? -1 : bInfo ? 1 : 0;
    }
    if (part) {
        const itemTypes = apViewSettings?.itemTypes || [];
        const aPosition = findViewItemType(itemTypes, part, aInfo.code)?.position;
        const bPosition = findViewItemType(itemTypes, part, bInfo.code)?.position;
        if (aPosition && bPosition && aPosition !== bPosition) {
            return aPosition - bPosition;
        } else if (aPosition && !bPosition) {
            return -1;
        } else if (!aPosition && bPosition) {
            return 1;
        }
    }
    return aInfo.viewOrder - bInfo.viewOrder;
}

export function compareItems(
    a: RevisionItem,
    b: RevisionItem,
    partTypeId: number,
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): number {
    return compareItemTypes(a.typeId, b.typeId, partTypeId, refTables, apViewSettings);
}

export function sortItems(
    partTypeId: number,
    items: RevisionItem[],
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): RevisionItem[] {
    return [...items].sort((a, b) => {
        return compareItems(a, b, partTypeId, refTables, apViewSettings);
    });
}

export function sortOwnItems(
    partTypeId: number,
    items: RevisionItem[],
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): RevisionItem[] {
    return items.sort((a, b) => {
        return compareItems(a, b, partTypeId, refTables, apViewSettings);
    });
}

export function findItemPlacePosition(
    item: RevisionItem,
    items: RevisionItem[],
    partTypeId: number,
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): number {
    // after the last item that precedes the new one
    const index = [...items]
        .reverse()
        .findIndex(
            (comparedItem) =>
                compareItemTypes(comparedItem.typeId, item.typeId, partTypeId, refTables, apViewSettings) < 0,
        );
    return index >= 0 ? items.length - index : 0;
}
