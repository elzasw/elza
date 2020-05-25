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

export function getPartType(partClass: string | undefined) : PartType {
  switch (partClass) {
    case AePartNameClass:
      return PartType.NAME;
    case AePartBodyClass:
      return PartType.BODY;
    case AePartCreationClass:
      return PartType.CRE;
    case AePartEventClass:
      return PartType.EVENT;
    case AePartExtinctionClass:
      return PartType.EXT;
    case AePartIdentifierClass:
      return PartType.IDENT;
    case AePartRelationClass:
      return PartType.REL;
    default:
      console.error("Chybí konverze z partclass na part type", partClass);
      return PartType.NAME;
  }
}
