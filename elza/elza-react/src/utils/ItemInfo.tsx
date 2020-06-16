import {RulDataTypeCodeEnum} from "../api/RulDataTypeCodeEnum";
import {ApItemVO} from "../api/ApItemVO";
import {PartType} from "../api/generated/model";
import {ApCreateTypeVO} from "../api/ApCreateTypeVO";
import {RulDescItemTypeExtVO} from "../api/RulDescItemTypeExtVO";
import {ApItemBitVO} from "../api/ApItemBitVO";
import {ApItemCoordinatesVO} from "../api/ApItemCoordinatesVO";
import {ApItemEnumVO} from "../api/ApItemEnumVO";
import {ApItemIntVO} from "../api/ApItemIntVO";
import {ApItemUriRefVO} from "../api/ApItemUriRefVO";
import {ApItemAccessPointRefVO} from "../api/ApItemAccessPointRefVO";
import {ApItemStringVO} from "../api/ApItemStringVO";
import {ApItemTextVO} from "../api/ApItemTextVO";
import {ApItemUnitdateVO} from "../api/ApItemUnitdateVO";
import {ApItemAPFragmentRefVO} from "../api/ApItemAPFragmentRefVO";
import {ApItemDateVO} from "../api/ApItemDateVO";
import {ApItemFormattedTextVO} from "../api/ApItemFormattedTextVO";
import {ApItemDecimalVO} from "../api/ApItemDecimalVO";
import {ApItemUnitidVO} from "../api/ApItemUnitidVO";

export const ApItemAccessPointRefClass = 'ApItemAccessPointRefVO';
export const ApItemAPFragmentRefClass = 'ApItemAPFragmentRefVO';
export const ApItemBitClass = 'ApItemBitVO';
export const ApItemCoordinatesClass = 'ApItemCoordinatesVO';
export const ApItemDateClass = 'ApItemDateVO';
export const ApItemDecimalClass = 'ApItemDecimalVO';
export const ApItemEnumClass = 'ApItemEnumVO';
export const ApItemFormattedTextClass = 'ApItemFormattedTextVO';
export const ApItemIntClass = 'ApItemIntVO';
export const ApItemJsonTableClass = 'ApItemJsonTableVO';
export const ApItemPartyRefClass = 'ApItemPartyRefVO';
export const ApItemStringClass = 'ApItemStringVO';
export const ApItemTextClass = 'ApItemTextVO';
export const ApItemUnitdateClass = 'ApItemUnitdateVO';
export const ApItemUnitidClass = 'ApItemUnitidVO';
export const ApItemUriRefClass = 'ApItemUriRefVO';

export function getItemClass(code: RulDataTypeCodeEnum): string {
    switch (code) {
        case RulDataTypeCodeEnum.RECORD_REF:
            return ApItemAccessPointRefClass;
        case RulDataTypeCodeEnum.APFRAG_REF:
            return ApItemAPFragmentRefClass;
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

export function compareItems(a: ApItemVO, b: ApItemVO, partType: PartType, refTables: any): number {
    let itemTypeInfoMap = refTables.partTypes.itemsMap[partType] || {};
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
        let aItemType = refTables.descItemTypes.itemsMap[a.typeId];
        let bItemType = refTables.descItemTypes.itemsMap[b.typeId];

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

export function compareCreateTypes(a: ApCreateTypeVO, b: ApCreateTypeVO, partType: PartType, refTables: any): number {
    let itemTypeInfoMap = refTables.partTypes.itemsMap[partType] || {};
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
        let aItemType = refTables.descItemTypes.itemsMap[a.itemTypeId];
        let bItemType = refTables.descItemTypes.itemsMap[b.itemTypeId];

        let n = aItemType.name.localeCompare(bItemType.name);
        if (n === 0) {
            return aItemType.code.localeCompare(bItemType.code);
        } else {
            return n;
        }
    }
}

export function sortItems(partType: PartType, items: ApItemVO[], refTables: any): ApItemVO[] {
    return [...items].sort((a, b) => {
        return compareItems(a, b, partType, refTables);
    });
}

export function sortOwnItems(partType: PartType, items: ApItemVO[], refTables: any): ApItemVO[] {
    return items.sort((a, b) => {
        return compareItems(a, b, partType, refTables);
    });
}

export function findItemPlacePosition(item: ApItemVO, items: ApItemVO[], partType: PartType, refTables: any): number {
    for (let index = items.length - 1; index >= 0; index--) {
        let i = items[index];
        let n = compareItems(item, i, partType, refTables);
        if (n >= 0) {
            return index + 1;
        }
    }

    return 0;
}

export function computeAllowedItemSpecIds(
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>,
    itemType: RulDescItemTypeExtVO,
    itemSpecId?: number
): number[] {
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
        case ApItemAccessPointRefClass:
            return typeof (item as ApItemAccessPointRefVO).value !== "undefined" && (item as ApItemAccessPointRefVO).value !== null;
        case ApItemAPFragmentRefClass:
            return typeof (item as ApItemAPFragmentRefVO).value !== "undefined" && (item as ApItemAPFragmentRefVO).value !== null;
        case ApItemBitClass:
            return typeof (item as ApItemBitVO).value !== "undefined" && (item as ApItemBitVO).value !== null;
        case ApItemCoordinatesClass:
            return !!(item as ApItemCoordinatesVO).value;
        case ApItemDateClass:
            return !!(item as ApItemDateVO).value;
        case ApItemDecimalClass:
            return typeof (item as ApItemDecimalVO).value !== "undefined" && (item as ApItemDecimalVO).value !== null;
        case ApItemEnumClass:
            return typeof (item as ApItemEnumVO).specId !== "undefined" && (item as ApItemEnumVO).specId !== null;
        case ApItemFormattedTextClass:
            return !!(item as ApItemFormattedTextVO).value;
        case ApItemIntClass:
            return typeof (item as ApItemIntVO).value !== "undefined" && (item as ApItemIntVO).value !== null;
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
        case ApItemPartyRefClass:
        default:
            console.error("Chybí podpora typu class", item["@class"]);
            return false;
    }
}
