import {PartType} from "../generated/model";
import {
  AePartBodyClass,
  AePartCreationClass,
  AePartEventClass,
  AePartExtinctionClass,
  AePartIdentifierClass,
  AePartNameClass,
  AePartRelationClass
} from "./ApPartInfo";

// TODO: na zahození, je třeba rozmyslet kde brát popisky
export function getPartEditDialogLabel(value: PartType, createDialog: boolean) {
  switch (value) {
    case PartType.BODY:
      return createDialog ? "Nové tělo" : "Upravit tělo";
    case PartType.CRE:
      return createDialog ? "Nový vznik" : "Upravit vznik";
    case PartType.EVENT:
      return createDialog ? "Nová událost" : "Upravit událost";
    case PartType.EXT:
      return createDialog ? "Nový zánik" : "Upravit zánik";
    case PartType.IDENT:
      return createDialog ? "Nový identifikátor" : "Upravit identifikátor";
    case PartType.NAME:
      return createDialog ? "Nové označení" : "Upravit označení";
    case PartType.REL:
      return createDialog ? "Nový vztah" : "Upravit vztah";
    default:
      console.warn("Nepřeložená hodnota", value);
      return "?";
  }
}
