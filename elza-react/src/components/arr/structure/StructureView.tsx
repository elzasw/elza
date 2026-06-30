import { DataType } from "elza-api";
import { useMemo } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { buildGroupsForm } from "../node-edit/utils";
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
} from "../node-view/desc-items";
import { Tooltip } from "@fluentui/react-components";
import { useStructureFormData } from "./hooks";

interface Props {
  fundId: number;
  fundVersionId: number;
  structureObjectId: number;
  plain?: boolean;
}

export type { Props as StructureViewProps };

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
};

export function StructureView({ fundId, fundVersionId, structureObjectId, plain = false }: Props) {
  const itemTypeRefs = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap,
  );
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);

  const { itemTypes, formItems } = useStructureFormData(
    fundId,
    fundVersionId,
    structureObjectId,
    { skipForcedItems: true },
  );

  const viewDescItemGroups = useMemo(() => {
    if (formItems && groupRefs) {
      return buildGroupsForm([...formItems], itemTypes, groupRefs, itemTypeRefs);
    }
    return [];
  }, [formItems, itemTypes, groupRefs, itemTypeRefs]);

  return (
    <div style={{ padding: "4px" }}>
      {viewDescItemGroups.map(({ group, descItemTypes }, groupIndex) => (
        <div key={groupIndex} style={{ margin: "4px" }}>
          {!plain && (
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
          )}
          <div
            style={
              plain
                ? { display: "flex", flexWrap: "wrap" }
                : {
                    padding: "16px",
                    background: "var(--shade-0)",
                    borderRadius: "8px",
                    boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
                    display: "flex",
                    flexWrap: "wrap",
                  }
            }
          >
            {descItemTypes.map(({ typeRef, typeForm, descItems }, typeIndex) => (
              <div
                key={typeIndex}
                style={{
                  verticalAlign: "top",
                  display: "flex",
                  flex: "wrap",
                  margin: "4px 16px 4px 4px",
                }}
              >
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
                <div>
                  {descItems.map(({ item }, itemIndex) => {
                    const { data } = item;
                    const DataTypeComponent =
                      data?.dataType && dataTypeMap[data.dataType];
                    return (
                      <div key={itemIndex} style={{ display: "flex" }}>
                        {item.itemSpecId && data?.dataType !== DataType.Enum && (
                          <div
                            style={{
                              marginRight: "4px",
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
                        <div style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                          {DataTypeComponent ? (
                            <DataTypeComponent
                              item={item}
                              nodeId={undefined}
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
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
