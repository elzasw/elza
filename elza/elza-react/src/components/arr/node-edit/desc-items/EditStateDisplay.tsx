import { EditFilled } from "@fluentui/react-icons";
import { FormattedMessage, defineMessages } from "react-intl";

const messages = defineMessages({
  unsaved: {
    id: "edit_state_unsaved",
    defaultMessage: "Neuloženo"
  }

})
export function EditStateDisplay() {
  return (
    <div
      style={{
        position: "absolute",
        top: "-18px",
        right: "4px",
        height: "20px",
        lineHeight: "1em",
        margin: "2px",
        padding: "2px",
        background: "var(--shade-0)",
        color: "var(--shade-7)",
        borderRadius: "4px",
        border: "1px solid var(--shade-6)",
        display: "flex",
        alignItems: "center",
      }}
    >
      <EditFilled />
      &nbsp;<FormattedMessage {...messages.unsaved} />
    </div>
  );
}
