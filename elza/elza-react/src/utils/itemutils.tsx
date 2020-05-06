import {
    AeItemLinkVO,
    AeItemRecordRefVO,
    ItemTypeVO,
    PartType,
} from "../api/generated/model";
import {
    AeItemBitClass,
    AeItemCoordinatesClass,
    AeItemEnumClass,
    AeItemIntegerClass,
    AeItemLinkClass,
    AeItemRecordRefClass,
    AeItemStringClass,
    AeItemTextClass,
    AeItemUnitdateClass
} from "../api/old/ItemInfo";
import {CodelistData} from "../types";
import {ApItemVO} from "../api/ApItemVO";
import {ApCreateTypeVO} from "../api/ApCreateTypeVO";
import {ApItemBitVO} from "../api/ApItemBitVO";
import {ApItemStringVO} from "../api/ApItemStringVO";
import {ApItemCoordinatesVO} from "../api/ApItemCoordinatesVO";
import {ApItemEnumVO} from "../api/ApItemEnumVO";
import {ApItemIntVO} from "../api/ApItemIntVO";
import {ApItemTextVO} from "../api/ApItemTextVO";
import {ApItemUnitdateVO} from "../api/ApItemUnitdateVO";


export function compareItems(a: ApItemVO, b: ApItemVO, partType: PartType, codelist: CodelistData): number {
    let itemTypeInfoMap = codelist.partItemTypeInfoMap[partType] || {};
    let aInfo = itemTypeInfoMap[a.typeId];
    let bInfo = itemTypeInfoMap[b.typeId];

    if (aInfo && bInfo) {
        if (aInfo.position && bInfo.position) {
            return aInfo.position - bInfo.position;
        } else if (aInfo.position && !bInfo.position) {
            return -1;
        } else if (!aInfo.position && bInfo.position) {
            return 1;
        } else {
            return 0;
        }
    } else if (aInfo && !bInfo) {
        return -1;
    } else if (!aInfo && bInfo) {
        return 1;
    } else {
        let aItemType = codelist.itemTypesMap[a.typeId];
        let bItemType = codelist.itemTypesMap[b.typeId];

        let n = aItemType.name.localeCompare(bItemType.name);
        if (n === 0) {
            if (a.id && b.id) {
                return (a.id || 0) - (b.id || 0);
            } else if (a.id && !b.id) {
                return -1;
            } else if (!a.id && b.id) {
                return 1;
            } else {
                return aItemType.code.localeCompare(bItemType.code);
            }
        } else {
            return n;
        }
    }
}

export function compareCreateTypes(a: ApCreateTypeVO, b: ApCreateTypeVO, partType: PartType, codelist: CodelistData): number {
    let itemTypeInfoMap = codelist.partItemTypeInfoMap[partType] || {};
    let aInfo = itemTypeInfoMap[a.itemTypeId];
    let bInfo = itemTypeInfoMap[b.itemTypeId];

    if (aInfo && bInfo) {
        if (aInfo.position && bInfo.position) {
            return aInfo.position - bInfo.position;
        } else if (aInfo.position && !bInfo.position) {
            return -1;
        } else if (!aInfo.position && bInfo.position) {
            return 1;
        } else {
            return 0;
        }
    } else if (aInfo && !bInfo) {
        return -1;
    } else if (!aInfo && bInfo) {
        return 1;
    } else {
        let aItemType = codelist.itemTypesMap[a.itemTypeId];
        let bItemType = codelist.itemTypesMap[b.itemTypeId];

        let n = aItemType.name.localeCompare(bItemType.name);
        if (n === 0) {
            return aItemType.code.localeCompare(bItemType.code);
        } else {
            return n;
        }
    }
}

export function sortItems(partType: PartType, items: Array<ApItemVO>, codelist: CodelistData): Array<ApItemVO> {
    return [...items].sort((a, b) => {
        return compareItems(a, b, partType, codelist);
    });
}

export function sortOwnItems(partType: PartType, items: Array<ApItemVO>, codelist: CodelistData) {
    return items.sort((a, b) => {
        return compareItems(a, b, partType, codelist);
    });
}

export function findItemPlacePosition(item: ApItemVO, items: Array<ApItemVO>, partType: PartType, codelist: CodelistData): number {
    for (let index = items.length - 1; index >= 0; index--) {
        let i = items[index];
        let n = compareItems(item, i, partType, codelist);
        if (n >= 0) {
            return index + 1;
        }
    }

    return 0;
}

export function computeAllowedItemSpecIds(
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    itemType: ItemTypeVO,
    itemSpecId?: number
): Array<number> {
    // Dohledání seznamu povolených specifikací dle atributu, pokud není atribut, použije se prázdný seznam
    // Pokud nějaká hodnota ze specifikace již zmizela (ze seznamu povolených hodnot), "umělě" se přidá do seznamu
    const attribute = itemTypeAttributeMap[itemType.id];
    let useItemSpecIds: Array<number>;
    if (attribute) {  // atribut existuje, vezmeme z něj seznam hodnot
        useItemSpecIds = [...(attribute.itemSpecIds || [])];
    } else {  // seznam hodnot bude prázdný
        useItemSpecIds = [];
    }
    if (itemSpecId && useItemSpecIds.indexOf(itemSpecId) < 0) { // má hodnotu specifikace, ale není v seznamu povolených, uměle ji tam přidáme
        useItemSpecIds.push(itemSpecId);
    }
    return useItemSpecIds;
}

export function hasItemValue(item: ApItemVO): boolean {
    switch (item["@class"]) {
        case AeItemBitClass:
            return typeof (item as ApItemBitVO).value !== "undefined" && (item as ApItemBitVO).value !== null;
        case AeItemCoordinatesClass:
            return !!(item as ApItemCoordinatesVO).value;
        case AeItemEnumClass:
            return typeof (item as ApItemEnumVO).specId !== "undefined" && (item as ApItemEnumVO).specId !== null;
        case AeItemIntegerClass:
            return typeof (item as ApItemIntVO).value !== "undefined" && (item as ApItemIntVO).value !== null;
        case AeItemLinkClass:
            return !!(item as AeItemLinkVO).value;
        case AeItemRecordRefClass:
            return typeof (item as AeItemRecordRefVO).value !== "undefined" && (item as AeItemRecordRefVO).value !== null;
        case AeItemStringClass:
            return !!(item as ApItemStringVO).value;
        case AeItemTextClass:
            return !!(item as ApItemTextVO).value;
        case AeItemUnitdateClass:
            return !!(item as ApItemUnitdateVO).value;
        default:
            console.error("Chybí podpora typu class", item["@class"]);
            return false;
    }
}
