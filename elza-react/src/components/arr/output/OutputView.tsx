import { DataType, NodeItem } from "elza-api";
import { useMemo } from "react";
import { Tooltip } from "@fluentui/react-components";
import { Api } from "api";
import { downloadBlob } from "actions/global/download";
import { useAppSelector } from "utils/hooks/useAppSelector";
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
} from "../node-view/desc-items";
import { useOutputFormData } from "./hooks";

interface Props {
  outputId: number;
}

export type { Props as OutputViewProps };

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

/**
 * Read-only zobrazení prvků popisu výstupu. Sdílí seskupení (buildGroupsForm) a zobrazovací
 * komponenty s náhledem uzlu (node-view), pouze je plní daty výstupu z useOutputFormData.
 */
export function OutputView({ outputId }: Props) {
  const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
  const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);

  async function exportCsv(item: NodeItem) {
    const { data } = await Api.output.outputOutputItemCsvExport(outputId, item.itemObjectId, {
      responseType: "blob",
    });
    downloadBlob(data, `output-${outputId}-${item.itemObjectId}.csv`);
  }

  const { formItems, forcedFormItems, addedFormItems, itemTypes } = useOutputFormData(outputId);

  const viewDescItemGroups = useMemo(() => {
    if (groupRefs) {
      return buildGroupsForm(
        [...formItems, ...forcedFormItems, ...addedFormItems],
        itemTypes,
        groupRefs,
        itemTypeRefs,
      );
    }
    return [];
  }, [formItems, forcedFormItems, addedFormItems, itemTypes, groupRefs, itemTypeRefs]);

  return (
    <div style={{ padding: "8px" }}>
      {viewDescItemGroups.map(({ group, descItemTypes }, groupIndex) => (
        <div key={groupIndex} style={{ margin: "4px" }}>
          <div style={{ opacity: 0.5, fontWeight: "bold", fontSize: "0.6rem", padding: "0 4px" }}>
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
            {descItemTypes.map(({ typeRef, typeForm, descItems }, typeIndex) => (
              <div
                key={typeIndex}
                style={{ verticalAlign: "top", display: "flex", flex: "wrap", margin: "4px 16px 4px 4px" }}
              >
                <Tooltip relationship="label" content={typeRef.description} appearance="inverted">
                  <div style={{ flexShrink: 1, fontWeight: "bold", marginRight: "4px" }}>
                    {typeRef.shortcut}:
                  </div>
                </Tooltip>
                <div>
                  {descItems.map(({ item }, itemIndex) => {
                    const { data } = item;
                    const DataTypeComponent = data?.dataType && dataTypeMap[data.dataType];
                    const specRef = typeRef.descItemSpecs.find(({ id }) => id === item.itemSpecId);
                    return (
                      <div key={itemIndex} style={{ display: "flex" }}>
                        {item.itemSpecId && data?.dataType !== DataType.Enum && (
                          <div
                            style={{
                              marginRight: "4px",
                              textDecoration: item.inhibited ? "line-through" : undefined,
                            }}
                          >
                            {specRef?.shortcut || specRef?.name || item.itemSpecId}:
                          </div>
                        )}
                        <div style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                          {DataTypeComponent ? (
                            <DataTypeComponent
                              item={item}
                              nodeId={item.nodeId}
                              typeRef={typeRef}
                              typeForm={typeForm}
                              onExportCsv={exportCsv}
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
