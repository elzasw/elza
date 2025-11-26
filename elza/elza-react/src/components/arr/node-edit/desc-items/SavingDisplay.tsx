import { Spinner } from "@fluentui/react-components";

interface Props {
  isSaving?: boolean;
}

export function SavingDisplay({ isSaving }: Props) {
  return isSaving ? (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        position: "absolute",
        width: "100%",
        height: "100%",
        background: "var(--shade-0)",
        opacity: "0.8",
      }}
    >
      <Spinner size="tiny" />
    </div>
  ) : (
    <></>
  );
}
