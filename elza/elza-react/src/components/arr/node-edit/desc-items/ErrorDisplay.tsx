import { makeStyles, Tooltip } from "@fluentui/react-components";
import { WarningFilled } from "@fluentui/react-icons";
import { useNodeFormContext } from "../NodeFormContext";
import { useUserSettings } from "contexts/user";
import { FIELD_HEIGHT } from "../../../../constants";

const useStyles = makeStyles({
  root: {
    color: "var(--color-orange)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "1.3em",
    padding: "0 4px",
  },
  tooltipItem: {
    margin: "4px",
  },
});

interface Props {
  itemObjectId?: number;
}

export function ErrorDisplay({ itemObjectId }: Props) {
  const { nodeData } = useNodeFormContext();
  const { settings } = useUserSettings();
  const size = settings.compact ? FIELD_HEIGHT.small : FIELD_HEIGHT.medium;
  const nodeConformity = nodeData?.nodeConformity;
  const styles = useStyles();

  const visibleErrors = (nodeConformity?.errorList ?? []).filter(
    ({ descItemObjectId, policyTypeId }) =>
      descItemObjectId === itemObjectId &&
      (policyTypeId == null || nodeConformity.viewPolicyTypeIds.includes(policyTypeId)),
  );

  return visibleErrors.length > 0 ? (
    <Tooltip
      appearance="inverted"
      content={visibleErrors.map(({ description }) => (
        <div className={styles.tooltipItem}>{description}</div>
      ))}
      relationship="description"
      withArrow={true}
    >
      <div
        className={styles.root}
        style={{ height: size + 'px' }}
      >
        <WarningFilled />
      </div>
    </Tooltip>
  ) : (
    <></>
  );
}
