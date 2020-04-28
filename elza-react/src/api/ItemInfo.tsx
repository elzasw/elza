import {
  AeItemBitVO,
  AeItemCoordinatesVO,
  AeItemEnumVO,
  AeItemIntegerVO, AeItemLinkVO, AeItemRecordRefVO, AeItemStringVO, AeItemTextVO, AeItemUnitdateVO,
  AeItemVO,
  PartType,
  SystemCode
} from "./generated/model";

export const AeItemBitClass = "AeItemBit";
export const AeItemCoordinatesClass = "AeItemCoordinates";
export const AeItemEnumClass = "AeItemEnum";
export const AeItemIntegerClass = "AeItemInteger";
export const AeItemLinkClass = "AeItemLink";
export const AeItemRecordRefClass = "AeItemRecordRef";
export const AeItemStringClass = "AeItemString";
export const AeItemTextClass = "AeItemText";
export const AeItemUnitdateClass = "AeItemUnitdate";

export function getItemClass(systemCode: SystemCode): string {
  switch (systemCode) {
    case SystemCode.BIT:
      return AeItemBitClass;
    case SystemCode.COORDINATES:
      return AeItemCoordinatesClass;
    case SystemCode.INTEGER:
      return AeItemIntegerClass;
    case SystemCode.LINK:
      return AeItemLinkClass;
    case SystemCode.NULL:
      return AeItemEnumClass;
    case SystemCode.RECORDREF:
      return AeItemRecordRefClass;
    case SystemCode.STRING:
      return AeItemStringClass;
    case SystemCode.TEXT:
      return AeItemTextClass;
    case SystemCode.UNITDATE:
      return AeItemUnitdateClass;
    default:
      console.error("Chybí konverze z systemCode na class", systemCode);
      return "";
  }
}
