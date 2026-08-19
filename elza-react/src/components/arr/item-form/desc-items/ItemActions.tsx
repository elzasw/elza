import { Button, Tooltip } from "@fluentui/react-components";
import {
  DismissRegular,
  EyeOffFilled,
  ArrowUndoRegular,
  DeleteRegular,
} from "@fluentui/react-icons";
import { WebApi } from "actions";
import { FormItemType, MandatoryType } from "elza-api";
import { EditItem } from "../types";
import { FormattedMessage, defineMessages } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useUserSettings } from "contexts/user";
import { useStyles } from "./styles";

interface Props {
  item: EditItem;
  nodeId?: number;
  specId?: number;
  isEnum: boolean;
  onDelete: () => void;
  onSetUndefined: () => void;
  typeForm?: FormItemType;
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
  isEnum,
  onDelete,
  onSetUndefined,
  typeForm,
  typeRef,
}: Props) {
  const { settings } = useUserSettings();
  const compact = settings.compact;
  const isInherited = item.nodeId != nodeId;
  const hasEnumValue = isEnum && item.itemSpecId != undefined;
  const hasValue = item.data?.dataId != undefined || item.undefined || hasEnumValue;
  const canSetUndefined = typeForm?.undefinable;
  const isOptional =
    typeForm?.type === MandatoryType.Possible ||
    typeForm?.type === MandatoryType.Impossible;

  // Enum reuses the specification field to hold its value, so useSpecification
  // is true from the backend; treat it as non-spec here.
  const useSpecification = typeRef.useSpecification && !isEnum;
  const spec = typeForm?.specs?.find(({ itemSpecId }) => itemSpecId === specId);
  const isSpecOptional =
    (useSpecification && !spec) ||
    spec?.type === MandatoryType.Possible ||
    spec?.type === MandatoryType.Impossible;

  function handleToggleInhibited() {
    if (item.inhibited) {
      WebApi.allowDescItem(nodeId, item.itemObjectId);
    } else {
      WebApi.inhibitDescItem(nodeId, item.itemObjectId);
    }
  }

  const styles = useStyles();
  return (
    <div className={styles.itemActions}>
      {!isInherited &&
        (hasValue || isOptional || (typeForm?.repeatable && isSpecOptional)) && !item.readOnly && (
          <Tooltip
            relationship="label"
            appearance="inverted"
            content={<FormattedMessage {...messages.delete} />}
          >
            <Button
              size={compact ? "small" : "medium"}
              appearance="subtle"
              icon={<DeleteRegular />}
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
            icon={item.inhibited ? <ArrowUndoRegular /> : <DeleteRegular />}
            onClick={() => handleToggleInhibited()}
            tabIndex={-1}
          />
        </Tooltip>
      )}
      {canSetUndefined &&
        !(useSpecification && !spec) &&
        !item.inhibited &&
        !item.undefined &&
        !item.data?.dataId &&
        !hasEnumValue && (
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
