import {
    AeCreateFormVO,
    AeItemBitVO,
    AeItemCoordinatesVO,
    AeItemEnumVO,
    AeItemIntegerVO,
    AeItemLinkVO,
    AeItemRecordRefVO,
    AeItemStringVO,
    AeItemTextVO, AeItemUnitdateVO,
    AeItemVO,
    AePartFormVO,
    AePartNameVO,
    AePartVO,
} from '../../../api/generated/model';
import {AePartNameClass} from "../../../api/AePartInfo";
import {
    AeItemBitClass,
    AeItemCoordinatesClass,
    AeItemEnumClass,
    AeItemIntegerClass,
    AeItemLinkClass, AeItemRecordRefClass, AeItemStringClass, AeItemTextClass, AeItemUnitdateClass
} from "../../../api/ItemInfo";

export function hasItemValue(item: AeItemVO): boolean {
    switch (item["@class"]) {
        case AeItemBitClass:
            return typeof (item as AeItemBitVO).value !== "undefined" && (item as AeItemBitVO).value !== null;
        case AeItemCoordinatesClass:
            return !!(item as AeItemCoordinatesVO).value;
        case AeItemEnumClass:
            return typeof (item as AeItemEnumVO).itemSpecId !== "undefined" && (item as AeItemEnumVO).itemSpecId !== null;
        case AeItemIntegerClass:
            return typeof (item as AeItemIntegerVO).value !== "undefined" && (item as AeItemIntegerVO).value !== null;
        case AeItemLinkClass:
            return !!(item as AeItemLinkVO).value;
        case AeItemRecordRefClass:
            return typeof (item as AeItemRecordRefVO).value !== "undefined" && (item as AeItemRecordRefVO).value !== null;
        case AeItemStringClass:
            return !!(item as AeItemStringVO).value;
        case AeItemTextClass:
            return !!(item as AeItemTextVO).value;
        case AeItemUnitdateClass:
            return !!(item as AeItemUnitdateVO).value;
        default:
            console.error("Chybí podpora typu class", item["@class"]);
            return false;
    }
    return true;
}

export function compareParts(a: AePartVO, b: AePartVO): number {
  if (a["@class"] == AePartNameClass) { // b by měla být stejná
    let aPreferred = (a as AePartNameVO).preferred;
    let bPreferred = (b as AePartNameVO).preferred;
    if (aPreferred && !bPreferred) {
      return -1;
    } else if (!aPreferred && bPreferred) {
      return 1;
    }
  }

  return a.textValue.localeCompare(b.textValue);
}

export function filterPartFormForSubmit(formData: AePartFormVO): AePartFormVO {
  const result: AePartFormVO = {
    ...formData,
    items: [
      ...formData.items
        .filter(hasItemValue)
    ]
  };
  return result;
}

export function filterCreateFormForSubmit(formData: AeCreateFormVO): AeCreateFormVO {
  const result: AeCreateFormVO = {
    ...formData,
    partForm: {
      ...formData.partForm,
      items: [
        ...formData.partForm.items
          .filter(hasItemValue)
      ]
    }
  };
  return result;
}
