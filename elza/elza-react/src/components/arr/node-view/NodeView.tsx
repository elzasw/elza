import { DataType, NodeFormData, NodeStatus } from "elza-api";
import { useMemo } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useNodeFormData } from "../node-edit/hooks";
import { buildGroupsForm } from "../item-form/utils";
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
} from "./desc-items";
import { Tooltip } from "@fluentui/react-components";

interface Props {
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
  /** When provided, NodeView waits for parent-supplied data instead of fetching. */
  seedFromParent?: boolean;
  seedFormData?: NodeFormData;
  seedNodeStatus?: NodeStatus;
  /** Called when the form requests a refresh (e.g. websocket NODES_CHANGE). */
  onRefresh?: () => void;
}

const dataTypeMap = {
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
  [DataType.JsonTable]: DescItemJsonTable,
};

export function NodeView({ fondsVersionId, nodeId, nodeVersionId, seedFromParent, seedFormData, seedNodeStatus, onRefresh }: Props) {
  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);

  const { itemTypes, formItems } = useNodeFormData(
    fondsVersionId,
    nodeId,
    nodeVersionId,
    {
      skipForcedItems: true,
      seedFromParent,
      seedFormData,
      seedNodeStatus,
      onRefresh,
    },
  );

  // build display groups only after groups refs and form data are both loaded
  const viewDescItemGroups = useMemo(() => {
    if (formItems && groupRefs) {
      return buildGroupsForm(
        [...formItems],
        itemTypes,
        groupRefs,
        itemTypeRefs,
      );
    }
    return [];
  }, [formItems, itemTypes, groupRefs, itemTypeRefs]);

  return (
    <div style={{ padding: "8px" /* , display: "flex", flexWrap: "wrap" */ }}>
      {viewDescItemGroups.map(({ group, descItemTypes }, groupIndex) => {
        return (
          <div key={groupIndex} style={{ margin: "4px" }}>
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
              style={{
                padding: "16px",
                background: "var(--shade-0)",
                borderRadius: "8px",
                boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
                display: "flex",
                flexWrap: "wrap",
              }}
            >
              {/* <table> */}
              {descItemTypes.map(({ typeRef, typeForm, descItems }, typeIndex) => {
                return (
                  <div
                    key={typeIndex}
                    style={{
                      verticalAlign: "top",
                      display: "flex",
                      flex: "wrap",
                      margin: "4px 16px 4px 4px",
                      // marginRight: "32px",
                    }}
                  >
                    {/* <td> */}
                    <Tooltip
                        relationship="label"
                        content={typeRef.description}
                        appearance="inverted"
                    >
                        <div
                            style={{
                                flexShrink: 1,
                                fontWeight: "bold",
                                marginRight: "4px",
                            }}
                        >
                            {typeRef.shortcut}:
                        </div>
                    </Tooltip>

                    {/* </td> */}
                    {/* <td> */}
                    <div>
                      {descItems.map(({ item }, itemIndex) => {
                        const { data } = item;
                        const DataTypeComponent =
                          data?.dataType && dataTypeMap[data.dataType];
                        const specRef = typeRef.descItemSpecs.find(
                          ({ id }) => id === item.itemSpecId,
                        )
                        return (
                          <div key={itemIndex} style={{ display: "flex" }}>
                            {item.itemSpecId &&
                              data?.dataType !== DataType.Enum && (
                                <div
                                  style={{
                                    marginRight: "4px",
                                    opacity:
                                      item.nodeId !== nodeId ? 0.5 : undefined,
                                    textDecoration: item.inhibited
                                      ? "line-through"
                                      : undefined,
                                  }}
                                >
                                  { specRef?.shortcut || specRef?.name || item.itemSpecId }
                                  :
                                </div>
                              )}
                            <div style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                              {DataTypeComponent ? (
                                <DataTypeComponent
                                  item={item}
                                  nodeId={nodeId}
                                  typeRef={typeRef}
                                  typeForm={typeForm}
                                />
                              ) : item.undefined ? (
                                "Nezjištěno"
                              ) : (
                                "Not implemented"
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                    {/* </td> */}
                  </div>
                );
              })}
              {/* </table> */}
            </div>
          </div>
        );
      })}
      {/* {nodeData?.descItems.map((item) => { */}
      {/*   const { data, itemTypeId } = item; */}
      {/**/}
      {/*   const DataTypeComponent = data?.dataType && dataTypeMap[data.dataType]; */}
      {/**/}
      {/*   return <div style={{ margin: "4px" }}> */}
      {/*     <div><b>{itemTypes[itemTypeId]?.name}</b></div> */}
      {/*     {DataTypeComponent ? <DataTypeComponent item={item} /> : "Not implemented"} */}
      {/*   </div> */}
      {/* })} */}
    </div>
  );
}
