import {
    AeCreateFormVO, AeItemLinkVO,
    ApPartFormVO,
    ApPartNameVO,
} from '../../../api/generated/model';
import {AePartNameClass} from "../../../api/old/ApPartInfo";
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
} from "../../../api/old/ItemInfo";
import {ApItemVO} from "../../../api/ApItemVO";
import {ApPartVO} from "../../../api/ApPartVO";
import {ApItemBitVO} from "../../../api/ApItemBitVO";
import {ApItemCoordinatesVO} from "../../../api/ApItemCoordinatesVO";
import {ApItemEnumVO} from "../../../api/ApItemEnumVO";
import {ApItemIntVO} from "../../../api/ApItemIntVO";
import {ApItemAccessPointRefVO} from "../../../api/ApItemAccessPointRefVO";
import {ApItemStringVO} from "../../../api/ApItemStringVO";
import {ApItemTextVO} from "../../../api/ApItemTextVO";
import {ApItemUnitdateVO} from "../../../api/ApItemUnitdateVO";

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
            return typeof (item as ApItemAccessPointRefVO).value !== "undefined" && (item as ApItemAccessPointRefVO).value !== null;
        case AeItemStringClass:
            return !!(item as ApItemStringVO).value;
        case AeItemTextClass:
            return !!(item as ApItemTextVO).value;
        case AeItemUnitdateClass:
            return !!(item as ApItemUnitdateVO).value;
            //todo: dodelat dalsi typy
        default:
            console.error("Chybí podpora typu class", item["@class"]);
            return false;
    }
}

export function compareParts(a: ApPartVO, b: ApPartVO): number {
  if (a["@class"] == AePartNameClass) { // b by měla být stejná
    let aPreferred = (a as ApPartNameVO).preferred;
    let bPreferred = (b as ApPartNameVO).preferred;
    if (aPreferred && !bPreferred) {
      return -1;
    } else if (!aPreferred && bPreferred) {
      return 1;
    }
  }

  return a.value.localeCompare(b.value);
}

export function filterPartFormForSubmit(formData: ApPartFormVO): ApPartFormVO {
    return {
        ...formData,
        items: [
            ...formData.items.filter(hasItemValue)
        ]
    };
}

export function filterCreateFormForSubmit(formData: AeCreateFormVO): AeCreateFormVO {
    return {
      ...formData,
      partForm: {
          ...formData.partForm,
          items: [
              ...formData.partForm.items
                  .filter(hasItemValue)
          ]
      }
  };
}
