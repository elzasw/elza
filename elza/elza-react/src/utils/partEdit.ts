import { RefTablesState } from 'typings/store';
import { ApItemVO } from '../api/ApItemVO';
import { ApViewSettingRule } from '../api/ApViewSettings';
import { RulPartTypeVO } from '../api/RulPartTypeVO';
import { findViewItemType } from './ItemInfo';
import { RevisionItem } from '../components/registry/revision';

export function compareItems(
    a: RevisionItem,
    b: RevisionItem,
    partTypeId: number,
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): number {
    const part: RulPartTypeVO | undefined = refTables.partTypes.itemsMap[partTypeId];
    const descItemTypesMap = refTables.descItemTypes.itemsMap;

    const aInfo = descItemTypesMap[a.typeId];
    const bInfo = descItemTypesMap[b.typeId];

    if (aInfo && bInfo && part) {
        let itemTypes = apViewSettings?.itemTypes || [];
        let aIt = findViewItemType(itemTypes, part, aInfo.code);
        let bIt = findViewItemType(itemTypes, part, bInfo.code);
        if (aIt && bIt) {
            if (aIt.position && bIt.position) {
                return aIt.position - bIt.position;
            } else if (aIt.position && !bIt.position) {
                return -1;
            } else if (!aIt.position && bIt.position) {
                return 1;
            } else {
                return 0;
            }
        } else if (aIt) {
            return -1;
        } else {
            return 1;
        }
    } else if (aInfo && !bInfo) {
        return -1;
    } else if (!aInfo && bInfo) {
        return 1;
    } else {
        return 0;
    }
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
    _partTypeId: number,
    refTables: RefTablesState,
    _apViewSettings?: ApViewSettingRule,
): number {
    const itemType = refTables.descItemTypes.itemsMap[item.typeId];
    const index = [...items].reverse().findIndex((comparedItem)=>{
        const comparedItemType = refTables.descItemTypes.itemsMap[comparedItem.typeId];
        return comparedItemType.viewOrder < itemType.viewOrder;
    })
    const finalIndex = index >= 0 ? items.length - index : 0;
    return finalIndex;
}
