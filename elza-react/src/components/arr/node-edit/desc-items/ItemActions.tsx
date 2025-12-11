import { Button } from "@fluentui/react-components";
import {
  DismissRegular,
  EyeOffFilled,
  ArrowUndoRegular,
} from "@fluentui/react-icons";
import { WebApi } from "actions";
import { FormItemType, MandatoryType, NodeItem } from "elza-api";
import { DescItemTypeRef } from "typings/store";

interface Props {
  item: NodeItem;
  nodeId: number;
  specId?: number;
  onDelete: () => void;
  onSetUndefined: () => void;
  typeForm: FormItemType;
  typeRef: DescItemTypeRef;
}

export function ItemActions({
  item,
  nodeId,
  specId,
  onDelete,
  onSetUndefined,
  typeForm,
  typeRef,
}: Props) {
  const isInherited = item.nodeId != nodeId;
  const hasValue = item.data?.dataId != undefined || item.undefined;
  const canSetUndefined = typeForm.undefinable;
  const isOptional =
    typeForm.type === MandatoryType.Possible ||
    typeForm.type === MandatoryType.Impossible;

  const spec = typeForm.specs.find(({ itemSpecId }) => itemSpecId === specId);
  const isSpecOptional =
    (typeRef.useSpecification && !spec) ||
    spec?.type === MandatoryType.Possible ||
    spec?.type === MandatoryType.Impossible;

  function handleToggleInhibited() {
    if (item.inhibited) {
      WebApi.allowDescItem(nodeId, item.itemObjectId);
    } else {
      WebApi.inhibitDescItem(nodeId, item.itemObjectId);
    }
  }

  return (
    <div style={{ display: "flex", alignItems: "flex-start" }}>
      {!isInherited &&
        (hasValue || isOptional || (typeForm.repeatable && isSpecOptional)) && (
          <Button
            appearance="subtle"
            icon={<DismissRegular />}
            onClick={() => onDelete()}
            tabIndex={-1}
          ></Button>
        )}
      {isInherited && (
        <Button
          appearance="subtle"
          icon={item.inhibited ? <ArrowUndoRegular /> : <DismissRegular />}
          onClick={() => handleToggleInhibited()}
          tabIndex={-1}
        ></Button>
      )}
      {canSetUndefined &&
        !(typeRef.useSpecification && !spec) &&
        !item.inhibited &&
        !item.undefined &&
        !item.data?.dataId && (
          <Button
            appearance="subtle"
            icon={<EyeOffFilled />}
            onClick={() => onSetUndefined()}
            tabIndex={-1}
          ></Button>
        )}
    </div>
  );
}
