import { Button, Tooltip } from "@fluentui/react-components";
import {
  DismissRegular,
  EyeOffFilled,
  ArrowUndoRegular,
} from "@fluentui/react-icons";
import { WebApi } from "actions";
import { FormItemType, MandatoryType, NodeItem } from "elza-api";
import { FormattedMessage, defineMessages } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useUserSettings } from "contexts/user";

interface Props {
  item: NodeItem;
  nodeId: number;
  specId?: number;
  onDelete: () => void;
  onSetUndefined: () => void;
  typeForm: FormItemType;
  typeRef: DescItemTypeRef;
}

const messages = defineMessages({
  delete: {
    id: "desc_item_action_delete",
    defaultMessage: "Odstranit hodnotu",
  },
  inhibit: {
    id: "desc_item_action_inhibit",
    defaultMessage: "Potlačit zděděnou hodnotu",
  },
  allow: {
    id: "desc_item_action_allow",
    defaultMessage: "Povolit zděděnou hodnotu",
  },
  setUndefined: {
    id: "desc_item_action_set_undefined",
    defaultMessage:
      "Nastavení hodnoty na 'výjimka' z důvodu nezjištění, neexistence nebo neuvádění na základě výjimky z pravidel",
  },
});

export function ItemActions({
  item,
  nodeId,
  specId,
  onDelete,
  onSetUndefined,
  typeForm,
  typeRef,
}: Props) {
  const { settings } = useUserSettings();
  const compact = settings.compact;
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
        (hasValue || isOptional || (typeForm.repeatable && isSpecOptional)) && !item.readOnly && (
          <Tooltip
            relationship="label"
            appearance="inverted"
            content={<FormattedMessage {...messages.delete} />}
          >
            <Button
              size={compact ? "small" : "medium"}
              appearance="subtle"
              icon={<DismissRegular />}
              onClick={() => onDelete()}
              tabIndex={-1}
            />
          </Tooltip>
        )}
      {isInherited && (
        <Tooltip
          relationship="label"
          appearance="inverted"
          content={
            item.inhibited ? (
              <FormattedMessage {...messages.allow} />
            ) : (
              <FormattedMessage {...messages.inhibit} />
            )
          }
        >
          <Button
            appearance="subtle"
            size={compact ? "small" : "medium"}
            icon={item.inhibited ? <ArrowUndoRegular /> : <DismissRegular />}
            onClick={() => handleToggleInhibited()}
            tabIndex={-1}
          />
        </Tooltip>
      )}
      {canSetUndefined &&
        !(typeRef.useSpecification && !spec) &&
        !item.inhibited &&
        !item.undefined &&
        !item.data?.dataId && (
          <Tooltip
            relationship="label"
            appearance="inverted"
            content={<FormattedMessage {...messages.setUndefined} />}
          >
            <Button
              size={compact ? "small" : "medium"}
              appearance="subtle"
              icon={<EyeOffFilled />}
              onClick={() => onSetUndefined()}
              tabIndex={-1}
            />
          </Tooltip>
        )}
    </div>
  );
}
