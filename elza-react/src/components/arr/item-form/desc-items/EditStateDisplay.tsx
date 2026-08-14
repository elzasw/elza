import { EditFilled } from "@fluentui/react-icons";
import { FormattedMessage, defineMessages } from "react-intl";
import { useStyles } from "./styles";

const messages = defineMessages({
  unsaved: {
    id: "edit_state_unsaved",
    defaultMessage: "Neuloženo"
  }

})
export function EditStateDisplay() {
  const styles = useStyles();
  return (
    <div className={styles.editStateDisplay}>
      <EditFilled />
      &nbsp;<FormattedMessage {...messages.unsaved} />
    </div>
  );
}
