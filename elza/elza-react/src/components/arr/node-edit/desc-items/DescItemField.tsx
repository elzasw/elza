import { Api } from "api";
import {
  DataType,
  FormItemType,
  ItemDataResult,
  NodeConformityError,
  NodeItem,
} from "elza-api";
import { useState } from "react";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import {
  DescItemBit,
  DescItemCoordinates,
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
} from ".";
import { DescItemSpec } from "./DescItemSpec";
import { ErrorDisplay } from "./ErrorDisplay";
import { ItemActions } from "./ItemActions";
import { SavingDisplay } from "./SavingDisplay";
import { createEmptyDescItem } from "./utils";

interface Props {
  item: NodeItem;
  typeRef: DescItemTypeRef;
  typeForm: FormItemType;
  typeWidth?: number;
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
  errors?: NodeConformityError[];
  onDelete?: (item: NodeItem) => Promise<void>;
  onCreate: (item: NodeItem) => Promise<ItemDataResult>;
  onUpdate: (item: NodeItem) => Promise<void>;
}

const dataTypeComponentMap = {
  [DataType.Text]: DescItemText,
  [DataType.Int]: DescItemInt,
  [DataType.Decimal]: DescItemDecimal,
  [DataType.Enum]: DescItemEnum,
  [DataType.String]: DescItemString,
  [DataType.Unitid]: DescItemUnitid,
  [DataType.Unitdate]: DescItemUnitdate,
  [DataType.RecordRef]: DescItemRecordRef,
  [DataType.Structured]: DescItemStructured,
  [DataType.Coordinates]: DescItemCoordinates,
  [DataType.FileRef]: DescItemFileRef,
  [DataType.UriRef]: DescItemUriRef,
  [DataType.Bit]: DescItemBit,
};

export function DescItemField({
  item,
  typeRef,
  typeForm,
  fondsVersionId,
  nodeId,
  errors = [],
  onDelete,
  onCreate,
  onUpdate,
  typeWidth,
}: Props) {
  const [specId, setSpecId] = useState<number | undefined>(item.itemSpecId);
  const [isSaving, setIsSaving] = useState(false);

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

  // update desc item with saved data when spec is changed
  function handleSpecChange(specId: number) {
    if (item?.data?.dataId && specId != item.itemSpecId) {
      setSpecId(specId);
      handleChange(item, specId);
    } else {
      setSpecId(specId);
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

  return (
    <div
      style={{
        display: "flex",
        margin: "4px 0",
        position: "relative",
        flex: 1,
        // alignItems: "center",
        alignItems: "flex-start",
      }}
    >
      <ErrorDisplay errors={errors} />
      {(item.itemSpecId || typeRef.useSpecification) && !isEnum && (
        <DescItemSpec
          isDisabled={item.undefined || item.nodeId != nodeId || item.inhibited}
          isInhibited={item.inhibited}
          typeForm={typeForm}
          typeRef={typeRef}
          value={specId}
          onChange={handleSpecChange}
          labelSource="name"
        />
      )}
      <div
        style={{
          whiteSpace: "pre-wrap",
          display: "flex",
          flex: dataTypeCode === DataType.Bit ? undefined : 1,
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
              !isEnum &&
              typeRef.useSpecification &&
              item.itemSpecId == undefined &&
              specId == undefined
            }
            selectedSpecId={specId}
            typeWidth={typeWidth}
          />
        ) : (
          "Not implemented"
        )}
      </div>
      <ItemActions
        item={item}
        nodeId={nodeId}
        specId={specId}
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
      <SavingDisplay isSaving={isSaving} />
    </div>
  );
}
