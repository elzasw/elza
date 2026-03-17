import { DescItemGroup } from "typings/store";
import { useStyles } from "./styles";
import { PropsWithChildren } from "react";

export interface Props extends PropsWithChildren {
  group: DescItemGroup;
}
export function FormItemGroup({ group, children }:Props){
  const styles = useStyles();

  return (
    <div style={{ margin: "4px" }} key={group.code}>
      <div
        style={{
          opacity: 0.5,
          fontWeight: "bold",
          fontSize: "0.6rem",
          padding: "0 4px",
        }}
      >
        {group.name}
      </div>
      <div
        className={styles.gridContainer}
        style={{
          padding: "8px",
          background: "var(--shade-0)",
          borderRadius: "8px",
          boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
          display: "grid",
          // gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
          // gridTemplateColumns: "repeat(4, 1fr)",
          // flexWrap: "wrap",
        }}
      >
        {children}
      </div>
    </div>
  );
}
