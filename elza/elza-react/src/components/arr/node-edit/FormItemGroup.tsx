import { mergeClasses } from "@fluentui/react-components";
import { DescItemGroup } from "typings/store";
import { useStyles } from "./styles";
import { PropsWithChildren } from "react";
import { useUserSettings } from "contexts/user";

export interface Props extends PropsWithChildren {
  group: DescItemGroup;
  plain?: boolean;
}
export function FormItemGroup({ group, plain = false, children }:Props){
  const styles = useStyles();
  const { settings } = useUserSettings();
  const compact = settings.compact;

  const gridClassName = mergeClasses(
    styles.gridContainer,
    !plain && (compact ? styles.gridContainerCompact : styles.gridContainerCard),
  );

  return (
    <div className={compact ? styles.groupWrapperCompact : styles.groupWrapper} key={group.code}>
      {!compact && !plain && <div className={styles.groupLabel}>{group.name}</div>}
      <div className={gridClassName}>
        {children}
      </div>
    </div>
  );
}
