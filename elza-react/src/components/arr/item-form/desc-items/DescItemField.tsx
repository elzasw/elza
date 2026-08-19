import { Api } from "api";
import {
  DataType,
  FormItemType,
  ItemDataResult,
  NodeItem,
} from "elza-api";
import { EditItem } from "../types";
import { useState } from "react";
import { useIntl } from "react-intl";
import { messages } from "../messages";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import {
  DescItemBit,
  DescItemCoordinates,
  DescItemDate,
  DescItemDecimal,
  DescItemEnum,
  DescItemFileRef,
  DescItemInt,
  DescItemJsonTable,
  DescItemRecordRef,
  DescItemString,
  DescItemStructured,
  DescItemText,
  DescItemUnitdate,
  DescItemUnitid,
  DescItemUriRef,
} from ".";
import { DescItemSpec } from "./DescItemSpec";
import { ErrorDisplay } from "./ErrorDisplay";
import { ItemActions } from "./ItemActions";
import { SavingDisplay } from "./SavingDisplay";
import { createEmptyDescItem } from "./utils";
import { Input, makeStyles, mergeClasses } from "@fluentui/react-components";
import { useUserSettings } from "contexts/user";
import { useStyles as useDescItemStyles } from "./styles";

const useStyles = makeStyles({
  descItem: {
    display: "flex",
    // flexDirection: "column",

    // "@container form-container (width > 600px)": {
      flexDirection: "row",
    // },
  },
  descItemInner: {
      display: "block",
      flex: 1,
      "& > *": {
          margin: 0,
          marginBottom: "4px",
      },

      "@container form-container (width > 500px)": {
          display: "flex",
          flexDirection: "row",
          "& > *": {
              margin: 0,
              marginRight: "4px",
          },
      },
  },
})

interface Props {
  item: EditItem;
  typeRef: DescItemTypeRef;
  typeForm?: FormItemType;
  typeWidth?: number;
  forcedDisplayString?: string;
  fondsVersionId?: number;
  nodeId?: number;
  nodeVersionId?: number;
  /** When true, the field is shown but cannot be edited (e.g. an automatically calculated item type). */
  readOnly?: boolean;
  onDelete?: (item: EditItem) => Promise<void>;
  onCreate: (item: EditItem) => Promise<ItemDataResult>;
  onUpdate: (item: EditItem) => Promise<void>;
  onExportCsv?: (item: EditItem) => void;
  onImportCsv?: (item: EditItem, file: File) => Promise<void>;
}

const dataTypeComponentMap = {
  [DataType.Text]: DescItemText,
  [DataType.Int]: DescItemInt,
  [DataType.Decimal]: DescItemDecimal,
  [DataType.Enum]: DescItemEnum,
  [DataType.String]: DescItemString,
  [DataType.Unitid]: DescItemUnitid,
  [DataType.Unitdate]: DescItemUnitdate,
  [DataType.Date]: DescItemDate,
  [DataType.RecordRef]: DescItemRecordRef,
  [DataType.Structured]: DescItemStructured,
  [DataType.Coordinates]: DescItemCoordinates,
  [DataType.FileRef]: DescItemFileRef,
  [DataType.UriRef]: DescItemUriRef,
  [DataType.Bit]: DescItemBit,
  [DataType.JsonTable]: DescItemJsonTable,
};

export function DescItemField({
  item,
  typeRef,
  typeForm,
  forcedDisplayString,
  fondsVersionId,
  nodeId,
  readOnly = false,
  onDelete,
  onCreate,
  onUpdate,
  onExportCsv,
  onImportCsv,
  typeWidth,
}: Props) {
  const [specId, setSpecId] = useState<number | undefined>(item.itemSpecId);
  const [isSaving, setIsSaving] = useState(false);
  const { settings } = useUserSettings();
  const compact = settings.compact;
  const { formatMessage } = useIntl();

  const styles = useStyles();
  const descItemStyles = useDescItemStyles();

  const { data } = item;

  const dataTypes = useAppSelector(
    ({ refTables }) => refTables.rulDataTypes.itemsMap,
  );
  const dataTypeCode = data?.dataType || dataTypes[typeRef.dataTypeId].code;
  const DataTypeComponent = dataTypeCode && dataTypeComponentMap[dataTypeCode];

  // const ws = useWebsocket();

  async function handleChange(_item: NodeItem, _specId?: number) {
    setIsSaving(true);
    let newItem: NodeItem = {
      ..._item,
      itemSpecId: _specId || specId || _item.itemSpecId,
    };

    // remove undefined flag, when item contains data
    if (
      _item.undefined &&
      (_item.data || (_item.itemSpecId && dataTypeCode === DataType.Enum))
    ) {
      if (_item.data.dataId || _item.undefined) {
        await Api.descItems.descItemDeleteDescItem(fondsVersionId, item);
      }

      newItem = createEmptyDescItem(
        _item.itemTypeId,
        _item.nodeId,
        _item.nodeVersion,
        _item.position,
        dataTypeCode,
      );

      newItem.itemSpecId = specId || _item.itemSpecId;
      newItem.data = { ...newItem.data, ..._item.data };
    }

    try {
      if (newItem.itemObjectId === undefined) {
        await onCreate(newItem);
      } else {
        await onUpdate(newItem);
      }
    } finally {
      setIsSaving(false);
    }
    return;
  }

  async function deleteDescItem(item: NodeItem) {
    setIsSaving(true);
    await onDelete(item);
    setIsSaving(false);
  }

  const isEnum = dataTypeCode === DataType.Enum;

  function handleSpecChange(newSpecId: number) {
    setSpecId(newSpecId);
    if (isEnum) {
      handleChange(item, newSpecId);
    } else {
      onUpdate({ ...item, itemSpecId: newSpecId });
    }
  }
  // useEffect(() => {
  //   if (
  //     item?.data?.dataId &&
  //     selectedSpec?.id != undefined &&
  //     selectedSpec?.id != item.itemSpecId
  //   ) {
  //     handleChange({ ...item, itemSpecId: selectedSpec?.id });
  //   }
  // }, [selectedSpec?.id]);
  // console.log("#dif - render", item);

  if (forcedDisplayString != undefined) {
    return (
      <div className={mergeClasses(styles.descItem, descItemStyles.descItemFieldRow)}>
        <Input
          size={compact ? "small" : "medium"}
          disabled={true}
          value={forcedDisplayString}
          style={{ flex: 1, minWidth: "60px" }}
        />
      </div>
    );
  }

  // Automatically calculated type with no value yet: show the "computed by function"
  // placeholder instead of an empty disabled field.
  if (readOnly && !item.data?.dataId) {
    return (
      <div className={mergeClasses(styles.descItem, descItemStyles.descItemFieldRow)}>
        <Input
          size={compact ? "small" : "medium"}
          disabled={true}
          value={formatMessage(messages.calculatedPlaceholder)}
          style={{ flex: 1, minWidth: "60px", fontStyle: "italic" }}
        />
      </div>
    );
  }

  return (
    <div
      className={mergeClasses(styles.descItem, descItemStyles.descItemFieldRow)}
    >
      {(item.itemSpecId || typeRef.useSpecification) && !isEnum && (
        <DescItemSpec
          isDisabled={readOnly || item.undefined || item.nodeId != nodeId || item.inhibited}
          isInhibited={item.inhibited}
          typeForm={typeForm}
          typeRef={typeRef}
          value={specId}
          onChange={handleSpecChange}
          labelSource="name"
          compact={compact}
        />
      )}
      <div
        style={{
          whiteSpace: "pre-wrap",
          display: "flex",
          flex: dataTypeCode === DataType.Bit ? undefined : 1,
          width: "100px",
        }}
      >
        {DataTypeComponent ? (
          <DataTypeComponent
            item={item}
            onChange={handleChange}
            typeForm={typeForm}
            typeRef={typeRef}
            nodeId={nodeId}
            isDisabled={
              readOnly ||
              (!isEnum &&
                typeRef.useSpecification &&
                item.itemSpecId == undefined &&
                specId == undefined)
            }
            selectedSpecId={specId}
            typeWidth={typeWidth}
            compact={compact}
            onExportCsv={onExportCsv}
            onImportCsv={onImportCsv}
          />
        ) : (
          "Not implemented"
        )}
      </div>
      <ErrorDisplay itemObjectId={item.itemObjectId} />
      {!readOnly && (
        <ItemActions
          item={item}
          nodeId={nodeId}
          specId={specId}
          isEnum={isEnum}
          typeForm={typeForm}
          typeRef={typeRef}
          onDelete={() => deleteDescItem(item)}
          onSetUndefined={() =>
            handleChange({
              ...item,
              undefined: true,
              data: undefined,
            })
          }
        />
      )}
      <SavingDisplay isSaving={isSaving} />
    </div>
  );
}
