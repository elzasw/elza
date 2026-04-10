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
    <div className={styles.groupWrapper} style={{ padding: compact ? "4px 4px" : "4px", containerName: "group-container", containerType: "inline-size" }} key={group.code}>
      {!compact && <div
        style={{
          opacity: 0.5,
          fontWeight: "bold",
          fontSize: "0.6rem",
          padding: "0 4px",
        }}
      >
        {group.name}
      </div>}
      <div
        className={compact ? styles.gridContainerCompact : styles.gridContainer}
        style={{
          padding: compact ? "4px 8px" : "8px",
          background: "var(--shade-0)",
          borderRadius: compact ? "4px" : "8px",
          boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
          display: "grid",
        }}
      >
        {children}
      </div>
    </div>
  );
}
