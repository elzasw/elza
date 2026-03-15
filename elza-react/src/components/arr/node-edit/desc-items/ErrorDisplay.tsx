import { Tooltip } from "@fluentui/react-components";
import { WarningFilled } from "@fluentui/react-icons";
import { useNodeFormContext } from "../NodeFormContext";

interface Props {
  itemObjectId?: number;
}

export function ErrorDisplay({ itemObjectId }: Props) {
  const { nodeData } = useNodeFormContext();
  const nodeConformity = nodeData?.nodeConformity;

  const visibleErrors = (nodeConformity?.errorList ?? []).filter(
    ({ descItemObjectId, policyTypeId }) =>
      descItemObjectId === itemObjectId &&
      (policyTypeId == null || nodeConformity.viewPolicyTypeIds.includes(policyTypeId)),
  );

  return visibleErrors.length > 0 ? (
    <Tooltip
      appearance="inverted"
      content={visibleErrors.map(({ description }) => (
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
