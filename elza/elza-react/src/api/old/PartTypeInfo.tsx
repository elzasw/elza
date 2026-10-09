import { RulPartTypeVO } from "../RulPartTypeVO";
import { getIntl } from "components/shared/lang/intlInstance";
import { partCreateMessages, partEditMessages } from "./partTypeMessages";

/**
 * Titulek dialogu pro založení nebo úpravu části entity.
 *
 * Sedm typů částí CAM má skloňované titulky; část jiného typu (typ části
 * deklarovaný balíčkem) dostane obecný titulek se jménem typu.
 */
export function getPartEditDialogLabel(partType: RulPartTypeVO, createDialog: boolean) {
  const descriptors = createDialog ? partCreateMessages : partEditMessages;
  const descriptor = descriptors[partType.code as keyof typeof descriptors];
  // Návratová hodnota jde do modalDialogShow, tedy mimo React strom.
  return descriptor
    ? getIntl().formatMessage(descriptor)
    : getIntl().formatMessage(descriptors.OTHER, { name: partType.name });
}
