import { CollapsibleDragWindow, FluentDialogContext } from "components/shared/dialog/FluentModalDialog";
import { useContext } from "react";
import { DeletedEntityWindow } from "./DeletedEntityWindow";
import { useIntl } from "react-intl";
import { messages } from "../messages";

export function useDeletedEntityWindow() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  const { formatMessage } = useIntl()

  return async function showModal() {
    console.log("#dew - hooks")
    return _showModal<undefined, undefined>({
      isSingleInstance: true,
      name: "deleted-entity-window",
      createDialog: ({ handleResult }) => {
        return <CollapsibleDragWindow
          title={formatMessage(messages.invalidatedEntities)}
          onClose={() => handleResult(undefined, undefined)}
          initialWidth={800}
        >
          <DeletedEntityWindow />
        </CollapsibleDragWindow>
      }
    })
  }
}
