import { Tooltip } from "@fluentui/react-components";
import { WarningFilled } from "@fluentui/react-icons";
import { NodeConformityError } from "elza-api";

interface Props {
  errors: NodeConformityError[];
}

export function ErrorDisplay({ errors }: Props) {
  return errors.length > 0 ? (
    <Tooltip
      appearance="inverted"
      content={errors.map(({ description }) => (
        <div style={{ margin: "4px" }}>{description}</div>
      ))}
      relationship="description"
      withArrow={true}
    >
      <div
        style={{
          padding: "4px",
          color: "var(--color-orange)",
          fontSize: "1.5em",
          display: "flex",
          alignItems: "center",
        }}
      >
              <WarningFilled style={{marginTop: "1px"}} />
      </div>
    </Tooltip>
  ) : (
    <></>
  );
}
