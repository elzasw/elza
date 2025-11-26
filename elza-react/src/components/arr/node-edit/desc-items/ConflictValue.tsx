import { Button } from "@fluentui/react-components";
import { ReactNode } from "react";

interface Props {
  value: string;
  conflictValue: string;
  isDirty: boolean;
  children: (conflictValue: string) => ReactNode;
  onResolve: (reset?: boolean) => void;
}

export function ConflictValue({ conflictValue, children, onResolve }: Props) {
  return (
    conflictValue && (
      <div style={{ display: "flex", flexDirection: "column" }}>
        <div
          style={{
            marginTop: "4px",
            display: "flex",
            justifyContent: "flex-end",
          }}
        >
          <Button appearance="primary" onClick={() => onResolve()}>
            Uložit
          </Button>
        </div>
        <label style={{ color: "var(--color-red)" }}>Konfliktní hodnota</label>
        {children(conflictValue)}
        <div
          style={{
            marginTop: "4px",
            display: "flex",
            justifyContent: "flex-end",
          }}
        >
          <Button appearance="outline" onClick={() => onResolve(true)}>
            Převzít
          </Button>
        </div>
      </div>
    )
  );
}
