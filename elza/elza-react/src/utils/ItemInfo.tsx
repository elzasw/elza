import {RulDataTypeCodeEnum} from '../api/RulDataTypeCodeEnum';
import {ApItemVO} from '../api/ApItemVO';
import {ApCreateTypeVO} from '../api/ApCreateTypeVO';
import {RulDescItemTypeExtVO} from '../api/RulDescItemTypeExtVO';
import {ApItemBitVO} from '../api/ApItemBitVO';
import {ApItemCoordinatesVO} from '../api/ApItemCoordinatesVO';
import {ApItemEnumVO} from '../api/ApItemEnumVO';
import {ApItemIntVO} from '../api/ApItemIntVO';
import {ApItemUriRefVO} from '../api/ApItemUriRefVO';
import {ApItemAccessPointRefVO} from '../api/ApItemAccessPointRefVO';
import {ApItemStringVO} from '../api/ApItemStringVO';
import {ApItemTextVO} from '../api/ApItemTextVO';
import {ApItemUnitdateVO} from '../api/ApItemUnitdateVO';
import {ApItemDateVO} from '../api/ApItemDateVO';
import {ApItemFormattedTextVO} from '../api/ApItemFormattedTextVO';
import {ApItemDecimalVO} from '../api/ApItemDecimalVO';
import {ApItemUnitidVO} from '../api/ApItemUnitidVO';
import {ApViewSettingRule, ItemType} from '../api/ApViewSettings';
import {RulPartTypeVO} from '../api/RulPartTypeVO';
import {RefTablesState} from 'typings/store';

export const ApItemAccessPointRefClass = '.ApItemAccessPointRefVO';
export const ApItemBitClass = '.ApItemBitVO';
export const ApItemCoordinatesClass = '.ApItemCoordinatesVO';
export const ApItemDateClass = '.ApItemDateVO';
export const ApItemDecimalClass = '.ApItemDecimalVO';
export const ApItemEnumClass = '.ApItemEnumVO';
export const ApItemFormattedTextClass = '.ApItemFormattedTextVO';
export const ApItemIntClass = '.ApItemIntVO';
export const ApItemJsonTableClass = '.ApItemJsonTableVO';
export const ApItemStringClass = '.ApItemStringVO';
export const ApItemTextClass = '.ApItemTextVO';
export const ApItemUnitdateClass = '.ApItemUnitdateVO';
export const ApItemUnitidClass = '.ApItemUnitidVO';
export const ApItemUriRefClass = '.ApItemUriRefVO';

export function getItemClass(code: RulDataTypeCodeEnum): string {
    switch (code) {
        case RulDataTypeCodeEnum.RECORD_REF:
            return ApItemAccessPointRefClass;
        case RulDataTypeCodeEnum.BIT:
            return ApItemBitClass;
        case RulDataTypeCodeEnum.COORDINATES:
            return ApItemCoordinatesClass;
        case RulDataTypeCodeEnum.DATE:
            return ApItemDateClass;
        case RulDataTypeCodeEnum.DECIMAL:
            return ApItemDecimalClass;
        case RulDataTypeCodeEnum.ENUM:
            return ApItemEnumClass;
        case RulDataTypeCodeEnum.FORMATTED_TEXT:
            return ApItemFormattedTextClass;
        case RulDataTypeCodeEnum.INT:
            return ApItemIntClass;
        case RulDataTypeCodeEnum.JSON_TABLE:
            return ApItemJsonTableClass;
        case RulDataTypeCodeEnum.STRING:
            return ApItemStringClass;
        case RulDataTypeCodeEnum.TEXT:
            return ApItemTextClass;
        case RulDataTypeCodeEnum.UNITDATE:
            return ApItemUnitdateClass;
        case RulDataTypeCodeEnum.UNITID:
            return ApItemUnitidClass;
        case RulDataTypeCodeEnum.URI_REF:
            return ApItemUriRefClass;

        //todo: Na tohle nemame classy + classa ApItemPartyRef nema kod
        case RulDataTypeCodeEnum.FILE_REF:
        case RulDataTypeCodeEnum.STRUCTURED:
        default:
            console.error('Chybí konverze z code na class', code);
            return '';
    }
}

export function compareItems(
    a: ApItemVO,
    b: ApItemVO,
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

export function compareCreateTypes(
    a: ApCreateTypeVO,
    b: ApCreateTypeVO,
    partTypeId: number,
    refTables: RefTablesState,
    apViewSettings: ApViewSettingRule,
): number {
    const part: RulPartTypeVO | undefined = refTables.partTypes.itemsMap[partTypeId];
    const descItemTypesMap = refTables.descItemTypes.itemsMap;

    const aInfo = descItemTypesMap[a.itemTypeId];
    const bInfo = descItemTypesMap[b.itemTypeId];

    if (aInfo && bInfo && part) {
        let itemTypes = apViewSettings.itemTypes;
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
    items: ApItemVO[],
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): ApItemVO[] {
    return [...items].sort((a, b) => {
        return compareItems(a, b, partTypeId, refTables, apViewSettings);
    });
}

export function sortOwnItems(
    partTypeId: number,
    items: ApItemVO[],
    refTables: RefTablesState,
    apViewSettings?: ApViewSettingRule,
): ApItemVO[] {
    return items.sort((a, b) => {
        return compareItems(a, b, partTypeId, refTables, apViewSettings);
    });
}

export function findItemPlacePosition(
    item: ApItemVO,
    items: ApItemVO[],
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

// export function findItemPlacePosition(
//     item: ApItemVO,
//     items: ApItemVO[],
//     partTypeId: number,
//     refTables: RefTablesState,
//     apViewSettings?: ApViewSettingRule,
// ): number {
//     for (let index = items.length - 1; index >= 0; index--) {
//         let i = items[index];
//         let n = compareItems(item, i, partTypeId, refTables, apViewSettings);
//         if (n >= 0) {
//             return index + 1;
//         }
//     }

//     return 0;
// }

export function findViewItemType(
    itemTypeSettings: ItemType[],
    part: RulPartTypeVO,
    itemTypeCode: string,
): ItemType | null {
    for (let i = 0; i < itemTypeSettings.length; i++) {
        const itemType = itemTypeSettings[i];
        if (itemType.partType === part.code && itemType.code === itemTypeCode) {
            return itemType;
        }
    }
    for (let i = 0; i < itemTypeSettings.length; i++) {
        const itemType = itemTypeSettings[i];
        if (itemType.partType == null && itemType.code === itemTypeCode) {
            return itemType;
        }
    }
    return null;
}

export function computeAllowedItemSpecIds(
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    itemType: RulDescItemTypeExtVO,
    itemSpecId?: number,
): number[] {
    // Dohledání seznamu povolených specifikací dle atributu, pokud není atribut, použije se prázdný seznam
    // Pokud nějaká hodnota ze specifikace již zmizela (ze seznamu povolených hodnot), "umělě" se přidá do seznamu
    const attribute = itemTypeAttributeMap[itemType.id];
    let useItemSpecIds: Array<number>;
    if (attribute) {
        // atribut existuje, vezmeme z něj seznam hodnot
        useItemSpecIds = [...(attribute.itemSpecIds || [])];
    } else {
        // seznam hodnot bude prázdný
        useItemSpecIds = [];
    }
    if (itemSpecId && useItemSpecIds.indexOf(itemSpecId) < 0) {
        // má hodnotu specifikace, ale není v seznamu povolených, uměle ji tam přidáme
        useItemSpecIds.push(itemSpecId);
    }
    return useItemSpecIds;
}

export function hasItemValue(item: ApItemVO): boolean {
    switch (item['@class']) {
        case ApItemAccessPointRefClass:
            return (
                typeof (item as ApItemAccessPointRefVO).value !== 'undefined' &&
                (item as ApItemAccessPointRefVO).value !== null
            );
        case ApItemBitClass:
            return typeof (item as ApItemBitVO).value !== 'undefined' && (item as ApItemBitVO).value !== null;
        case ApItemCoordinatesClass:
            return !!(item as ApItemCoordinatesVO).value;
        case ApItemDateClass:
            return !!(item as ApItemDateVO).value;
        case ApItemDecimalClass:
            return typeof (item as ApItemDecimalVO).value !== 'undefined' && (item as ApItemDecimalVO).value !== null;
        case ApItemEnumClass:
            return typeof (item as ApItemEnumVO).specId !== 'undefined' && (item as ApItemEnumVO).specId !== null;
        case ApItemFormattedTextClass:
            return !!(item as ApItemFormattedTextVO).value;
        case ApItemIntClass:
            return typeof (item as ApItemIntVO).value !== 'undefined' && (item as ApItemIntVO).value !== null;
        case ApItemStringClass:
            return !!(item as ApItemStringVO).value;
        case ApItemTextClass:
            return !!(item as ApItemTextVO).value;
        case ApItemUnitdateClass:
            return !!(item as ApItemUnitdateVO).value;
        case ApItemUnitidClass:
            return !!(item as ApItemUnitidVO).value;
        case ApItemUriRefClass:
            return !!(item as ApItemUriRefVO).value;

        case ApItemJsonTableClass:
        default:
            console.error('Chybí podpora typu class', item['@class']);
            return false;
    }
}
