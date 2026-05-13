import { Button } from "@fluentui/react-components";
import { ReactNode } from "react";
import { useStyles } from "./styles";

interface Props {
  value: string;
  conflictValue: string;
  isDirty: boolean;
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
            Uložit
          </Button>
        </div>
        <label className={styles.conflictLabel}>Konfliktní hodnota</label>
        {children(conflictValue)}
        <div className={styles.conflictActions}>
          <Button appearance="outline" onClick={() => onResolve(true)}>
            Převzít
          </Button>
        </div>
      </div>
    )
  );
}
