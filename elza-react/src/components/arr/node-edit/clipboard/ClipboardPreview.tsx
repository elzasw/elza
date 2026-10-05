import { Button, Divider, Tooltip, makeStyles, tokens } from "@fluentui/react-components";
import { DeleteRegular } from "@fluentui/react-icons";
import { DataJsonTable, DataStructureRef, DataType, DataFileRef } from "elza-api";
import { ComponentType } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { messages as commonMessages } from "components/arr/item-form/desc-items/commonMessages";
import {
  DescItemBit,
  DescItemCoordinates,
  DescItemDate,
  DescItemDecimal,
  DescItemEnum,
  DescItemFileRef,
  DescItemInt,
  DescItemRecordRef,
  DescItemString,
  DescItemStructured,
  DescItemText,
  DescItemUnitdate,
  DescItemUnitid,
  DescItemUriRef,
} from "../../node-view/desc-items";
import { DescItemProps } from "../../node-view/desc-items/types";
import { ClipboardItem } from "./itemClipboard";
import { clipboardMessages } from "./messages";

// The view components fade values whose nodeId differs from the shown node's; copied items get one shared id.
const PREVIEW_NODE_ID = 0;

// Same components as NodeView, except JSON tables, which are summarized by their row count.
const valueComponents: Partial<Record<string, ComponentType<DescItemProps>>> = {
  [DataType.Text]: DescItemText,
  [DataType.Int]: DescItemInt,
  [DataType.Decimal]: DescItemDecimal,
  [DataType.Enum]: DescItemEnum,
  [DataType.String]: DescItemString,
  [DataType.Unitid]: DescItemUnitid,
  [DataType.Unitdate]: DescItemUnitdate,
  [DataType.Date]: DescItemDate,
  [DataType.RecordRef]: DescItemRecordRef,
  [DataType.UriRef]: DescItemUriRef,
  [DataType.Coordinates]: DescItemCoordinates,
  [DataType.Structured]: DescItemStructured,
  [DataType.FileRef]: DescItemFileRef,
  [DataType.Bit]: DescItemBit,
};

const useStyles = makeStyles({
  title: {
    fontWeight: tokens.fontWeightSemibold,
  },
  divider: {
    marginBlock: tokens.spacingVerticalXS,
  },
  footer: {
    display: "flex",
    justifyContent: "flex-end",
    marginTop: tokens.spacingVerticalXS,
  },
  row: {
    display: "flex",
    columnGap: tokens.spacingHorizontalXS,
    marginBlock: "2px",
  },
  label: {
    flexShrink: 0,
    fontWeight: tokens.fontWeightBold,
  },
  values: {
    flexGrow: 1,
  },
  value: {
    display: "flex",
    alignItems: "flex-start",
    columnGap: tokens.spacingHorizontalXS,
  },
  removeButton: {
    flexShrink: 0,
    marginInlineStart: "auto",
  },
  clamped: {
    display: "-webkit-box",
    WebkitLineClamp: 3,
    WebkitBoxOrient: "vertical",
    overflow: "hidden",
    whiteSpace: "pre-wrap",
    wordBreak: "break-word",
  },
});

function countTableRows(data: DataJsonTable) {
  try {
    const parsed = JSON.parse(data.value);
    return Array.isArray(parsed?.rows) ? parsed.rows.length : 0;
  } catch {
    return 0;
  }
}

interface ValueProps {
  item: ClipboardItem;
  typeRef: DescItemTypeRef;
  isOtherFund: boolean;
}

function PreviewValue({ item, typeRef, isOtherFund }: ValueProps) {
  const data = item.data;
  if (item.undefined || !data) {
    return <FormattedMessage {...commonMessages.undefined} />;
  }
  if (data.dataType === DataType.JsonTable) {
    return <FormattedMessage {...clipboardMessages.tableRows} values={{ count: countTableRows(data as DataJsonTable) }} />;
  }
  // Structure and file references load their details from the active fund.
  if (isOtherFund && data.dataType === DataType.Structured) {
    return <>{(data as DataStructureRef).value}</>;
  }
  if (isOtherFund && data.dataType === DataType.FileRef) {
    return <>{(data as DataFileRef).fileId}</>;
  }
  const ValueComponent = valueComponents[data.dataType];
  if (!ValueComponent) {
    return null;
  }
  return <ValueComponent item={{ ...item, nodeId: PREVIEW_NODE_ID }} nodeId={PREVIEW_NODE_ID} typeRef={typeRef} />;
}

interface Props {
  title: string;
  items: ClipboardItem[];
  /** The clipboard comes from another fund than the active one. */
  isOtherFund: boolean;
  /** Removes the item at this index of `items`. */
  onRemoveItem: (index: number) => void;
  onClear: () => void;
}

/** Lists the copied DescItems grouped by type, in the form's order, like NodeView. */
export function ClipboardPreview({ title, items, isOtherFund, onRemoveItem, onClear }: Props) {
  const styles = useStyles();
  const { formatMessage } = useIntl();
  const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);

  const typeIds = Array.from(new Set(items.map(({ itemTypeId }) => itemTypeId)))
    .filter((itemTypeId): itemTypeId is number => itemTypeId != undefined && itemTypeRefs[itemTypeId] != undefined)
    .sort((a, b) => itemTypeRefs[a].viewOrder - itemTypeRefs[b].viewOrder);
  const indexedItems = items.map((item, index) => ({ item, index }));
  const removeItemLabel = formatMessage(clipboardMessages.removeItem);

  return (
    <div>
      <div className={styles.title}>{title}</div>
      <Divider className={styles.divider} />
      {typeIds.map((itemTypeId) => {
        const typeRef = itemTypeRefs[itemTypeId];
        const typeItems = indexedItems.filter(({ item }) => item.itemTypeId === itemTypeId);
        return (
          <div key={itemTypeId} className={styles.row}>
            <div className={styles.label}>{typeRef.shortcut}:</div>
            <div className={styles.values}>
              {typeItems.map(({ item, index }) => {
                const specRef = typeRef.descItemSpecs?.find(({ id }) => id === item.itemSpecId);
                const hasSpecPrefix = specRef != undefined && item.data?.dataType !== DataType.Enum;
                return (
                  <div key={index} className={styles.value}>
                    {hasSpecPrefix && <span>{specRef.shortcut || specRef.name}:</span>}
                    <div className={styles.clamped}>
                      <PreviewValue item={item} typeRef={typeRef} isOtherFund={isOtherFund} />
                    </div>
                    <Tooltip relationship="label" content={removeItemLabel}>
                      <Button
                        className={styles.removeButton}
                        size="small"
                        appearance="subtle"
                        icon={<DeleteRegular />}
                        onClick={() => onRemoveItem(index)}
                      />
                    </Tooltip>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
      <div className={styles.footer}>
        <Button size="small" appearance="primary" onClick={onClear}>
          {formatMessage(clipboardMessages.clear)}
        </Button>
      </div>
    </div>
  );
}
