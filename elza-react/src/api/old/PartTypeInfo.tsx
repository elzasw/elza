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
import { getIntl } from "components/shared/lang/intlInstance";
import { partCreateMessages, partEditMessages } from "./partTypeMessages";

// TODO: na zahození, je třeba rozmyslet kde brát popisky
export function getPartEditDialogLabel(value: PartType, createDialog: boolean) {
  const descriptors = createDialog ? partCreateMessages : partEditMessages;
  const descriptor = descriptors[value as keyof typeof descriptors];
  if (!descriptor) {
    console.warn("Nepřeložená hodnota", value);
    return "?";
  }
  // Návratová hodnota jde do modalDialogShow, tedy mimo React strom.
  return getIntl().formatMessage(descriptor);
}
