import { Spinner } from "@fluentui/react-components";
import { useStyles } from "./styles";

interface Props {
  isSaving?: boolean;
}

export function SavingDisplay({ isSaving }: Props) {
  const styles = useStyles();
  return isSaving ? (
    <div className={styles.savingOverlay}>
      <Spinner size="tiny" />
    </div>
  ) : (
    <></>
  );
}
