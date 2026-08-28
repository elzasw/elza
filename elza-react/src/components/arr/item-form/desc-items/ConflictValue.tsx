import { Button } from "@fluentui/react-components";
import { ReactNode } from "react";
import { FormattedMessage, defineMessages } from "react-intl";
import { useStyles } from "./styles";

const messages = defineMessages({
  label: {
    id: "itemForm.conflict.label",
    defaultMessage: "Konfliktní hodnota",
  },
  save: {
    id: "itemForm.conflict.save",
    defaultMessage: "Uložit",
  },
  takeOver: {
    id: "itemForm.conflict.takeOver",
    defaultMessage: "Převzít",
  },
});

interface Props {
  conflictValue: string;
  isValid?: boolean;
  children: (conflictValue: string) => ReactNode;
  onResolve: (reset?: boolean) => void;
}

export function ConflictValue({ conflictValue, children, onResolve, isValid = true }: Props) {
  const styles = useStyles();
  return (
    conflictValue && (
      <div className={styles.conflictOuter}>
        <div className={styles.conflictActions}>
          <Button appearance="primary" onClick={() => onResolve()} disabled={!isValid}>
            <FormattedMessage {...messages.save} />
          </Button>
        </div>
        <label className={styles.conflictLabel}>
          <FormattedMessage {...messages.label} />
        </label>
        {children(conflictValue)}
        <div className={styles.conflictActions}>
          <Button appearance="outline" onClick={() => onResolve(true)}>
            <FormattedMessage {...messages.takeOver} />
          </Button>
        </div>
      </div>
    )
  );
}
