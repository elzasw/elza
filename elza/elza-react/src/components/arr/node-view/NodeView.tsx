import { DataType } from "elza-api";
import { useMemo } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useNodeFormData } from "../node-edit/hooks";
import { buildGroups } from "../node-edit/utils";
import {
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
} from "./desc-items";

interface Props {
  fondsVersionId: number;
  nodeId: number;
  nodeVersionId: number;
}

const dataTypeMap = {
  [DataType.Text]: DescItemText,
  [DataType.Int]: DescItemInt,
  [DataType.Decimal]: DescItemDecimal,
  [DataType.Enum]: DescItemEnum,
  [DataType.String]: DescItemString,
  [DataType.Unitid]: DescItemUnitid,
  [DataType.Unitdate]: DescItemUnitdate,
  [DataType.RecordRef]: DescItemRecordRef,
  [DataType.UriRef]: DescItemUriRef,
  [DataType.Coordinates]: DescItemCoordinates,
  [DataType.Structured]: DescItemStructured,
  [DataType.FileRef]: DescItemFileRef,
};

export function NodeView({ fondsVersionId, nodeId, nodeVersionId }: Props) {
  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);
  const dataTypeRefs = useAppSelector(
    ({ refTables }) => refTables.rulDataTypes.itemsMap,
  );

  const { formData } = useNodeFormData(fondsVersionId, nodeId, nodeVersionId);

  // build display groups only after groups refs and form data are both loaded
  const viewDescItemGroups = useMemo(() => {
    if (formData && groupRefs) {
      return buildGroups(
        formData,
        groupRefs,
        itemTypeRefs,
        dataTypeRefs,
        nodeId,
        nodeVersionId,
        true,
      );
    }
    return [];
  }, [formData, groupRefs, itemTypeRefs, dataTypeRefs, nodeId, nodeVersionId]);

  return (
    <div style={{ padding: "8px" /* , display: "flex", flexWrap: "wrap" */ }}>
      {viewDescItemGroups.map(({ group, descItemTypes }) => {
        return (
          <div style={{ margin: "4px" }}>
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
              {descItemTypes.map(({ typeRef, typeForm, descItems }) => {
                return (
                  <div
                    style={{
                      verticalAlign: "top",
                      display: "flex",
                      flex: "wrap",
                      margin: "4px 16px 4px 4px",
                      // marginRight: "32px",
                    }}
                  >
                    {/* <td> */}
                    <div
                      style={{
                        flexShrink: 1,
                        fontWeight: "bold",
                        marginRight: "4px",
                      }}
                    >
                      {typeRef.shortcut}:
                    </div>

                    {/* </td> */}
                    {/* <td> */}
                    <div>
                      {descItems.map((item) => {
                        const { data } = item;
                        const DataTypeComponent =
                          data?.dataType && dataTypeMap[data.dataType];
                        return (
                          <div style={{ display: "flex" }}>
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
                                  {
                                    typeRef.descItemSpecs.find(
                                      ({ id }) => id === item.itemSpecId,
                                    )?.shortcut
                                  }
                                  :
                                </div>
                              )}
                            <div style={{ whiteSpace: "pre-wrap" }}>
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
