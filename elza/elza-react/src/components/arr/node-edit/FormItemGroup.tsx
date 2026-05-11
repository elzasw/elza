import { DescItemGroup } from "typings/store";
import { useStyles } from "./styles";
import { PropsWithChildren } from "react";
import { useUserSettings } from "contexts/user";

export interface Props extends PropsWithChildren {
  group: DescItemGroup;
}
export function FormItemGroup({ group, children }:Props){
  const styles = useStyles();
  const { settings } = useUserSettings();
  const compact = settings.compact;

  return (
    <div className={compact ? styles.groupWrapperCompact : styles.groupWrapper} key={group.code}>
      {!compact && <div className={styles.groupLabel}>{group.name}</div>}
      <div className={compact ? styles.gridContainerCompact : styles.gridContainer}>
        {children}
      </div>
    </div>
  );
}
