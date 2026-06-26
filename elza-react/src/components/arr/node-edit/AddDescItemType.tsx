import React, { useEffect, useRef, useState } from "react";
import { i18n, ModalDialogWrapper } from "components/shared";
import { Form, FormGroup, FormLabel, Modal, Button } from "react-bootstrap";
import { ItemTypeField } from "components/arr/nodeForm/ItemTypeField";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { WebApi } from "actions";
import { FormItemType, MandatoryType } from "elza-api";
import { EditItem } from "./types";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { makeStyles, mergeClasses, Text, tokens } from "@fluentui/react-components";
import { CheckmarkRegular, DismissRegular, InfoRegular } from "@fluentui/react-icons";
import ListItem from "components/shared/tree-list/list-item/ListItem.jsx";
import { getOneSettings } from "../ArrUtils";
import { useInitialFocus } from "../search-funds-form/filters/utils";
import { defineMessages, useIntl } from "react-intl";

/**
 * Formulář přidání nové desc item type.
 */
interface Props {
  itemTypes: FormItemType[];
  descItems: EditItem[];
  onSubmit: (descItemTypes: DescItemTypeRef[]) => void;
  onClose: () => void;
}

const useStyles = makeStyles({
  addLink: {
    backgroundColor: "var(--shade-2)",
    color: "var(--fg-color)",
    textDecoration: "none",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    border: "none",
    margin: "2px",
    borderRadius: "3px",
    ":hover": {
      backgroundColor: "var(--shade-5)",
      color: "var(--fg-color)",
      textDecoration: "none",
    },
  },
  queued: {
    backgroundColor: "var(--accent-color)",
    color: "var(--accent-color-fg)",
    ":hover": {
      backgroundColor: "var(--accent-color)",
      color: "var(--accent-color-fg)",
    },
  },
  queuedImpossible: {
    backgroundColor: "var(--color-red)",
    ":hover": {
      backgroundColor: "var(--color-red)",
    },
  },
  dismissIcon: {
    marginLeft: "4px",
  },
  itemName: {
    display: "inline-flex",
    alignItems: "center",
    gap: "4px",
  },
  queuedCheck: {
    color: "var(--fg-color)",
  },
  queuedCheckHidden: {
    visibility: "hidden",
  },
});

const messages = defineMessages({
  addDescItemFormTitle: {
    id: "add_desc_item_form_title",
    defaultMessage: "Přidat prvek popisu",
  },
  multiAddHint: {
    id: "add_desc_item_form_multi_hint",
    defaultMessage: "Tip: použijte CTRL pro výběr více prvků z nabídky, nebo vyberte prvek z pole našeptávače (ENTER na prázdném poli potvrdí výběr)",
  },
});

export function AddDescItemTypeForm({ itemTypes, descItems, onSubmit, onClose }: Props) {
  const [selectedItemType, setSelectedItem] = useState<DescItemTypeRef>();
  const [queuedItemTypes, setQueuedItemTypes] = useState<DescItemTypeRef[]>([]);
  const descItemTypes = useAppSelector(({ refTables }) => refTables.descItemTypes.items);
  const descItemGroups = useAppSelector(({ refTables }) => refTables.groups.data);
  const strictMode: boolean = useAppSelector(({ userDetail, arrRegion }) => {
    const activeFund = arrRegion.funds[arrRegion.activeIndex];
    const strictModeSetting = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', activeFund.id);
    const strictModeValue = JSON.parse(strictModeSetting.value);
    return strictModeValue ?? true;
  })
  const ruleSetId = useAppSelector(({ arrRegion }) => arrRegion.funds[arrRegion.activeIndex]?.activeVersion?.ruleSetId);

  // In non-strict mode impossible types may be added too. They are absent from the per-node itemTypes,
  // so the rule set's item type codes provide the set of node-compatible types to offer.
  const [ruleSetItemTypeCodes, setRuleSetItemTypeCodes] = useState<string[]>();
  useEffect(() => {
    if (strictMode || ruleSetId == null) {
      return;
    }
    let cancelled = false;
    (async () => {
      const codes: string[] = await WebApi.getItemTypeCodesByRuleSet(ruleSetId);
      if (!cancelled) {
        setRuleSetItemTypeCodes(codes);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [strictMode, ruleSetId]);

  const { formatMessage } = useIntl();

  const inputRef = useRef(null);
  useInitialFocus(inputRef);

  const styles = useStyles();

  function getPossibleItemTypes() {
    const possibleTypes = descItemTypes.filter(({ id }) => {
      const isAdded = !!descItems.find(({ itemTypeId }) => itemTypeId === id);
      if (isAdded) {
        return false;
      }
      // Impossible types have no button by default; show one once it has been queued via the input.
      const isQueued = queuedItemTypes.some(({ id: queuedId }) => queuedId === id);
      const isPossible = !!itemTypes.find(
        ({ itemTypeId, type }) => itemTypeId === id && type === MandatoryType.Possible,
      );
      return isPossible || isQueued;
    });
    const possibleGroups: Array<DescItemGroup & { children: DescItemTypeRef[] }> = [];
    descItemGroups.ids.forEach((groupId) => {
      const group = descItemGroups[groupId];
      const children = possibleTypes.filter(({ id }) => group.itemTypes.find((itemType) => itemType.id === id));
      if (children.length > 0) {
        possibleGroups.push({ ...group, children });
      }
    });
    return possibleGroups;
  }

  // Selecting an item in the autocomplete (Enter or click) queues it and clears the field, so a
  // second Enter on the now-empty field submits the dialog.
  function handleChange(itemType: DescItemTypeRef) {
    if (!itemType) {
      setSelectedItem(undefined);
      return;
    }
    toggleQueued(itemType);
    setSelectedItem(undefined);
  }

  function addAndClose(itemType: DescItemTypeRef) {
    onSubmit([itemType]);
    onClose();
  }

  function toggleQueued(itemType: DescItemTypeRef) {
    setQueuedItemTypes((queued) =>
      queued.some(({ id }) => id === itemType.id)
        ? queued.filter(({ id }) => id !== itemType.id)
        : [...queued, itemType],
    );
  }

  // Once a queue is being built (Ctrl/Cmd+click, or any click while the queue is non-empty), clicks
  // toggle the type in the queue and keep the dialog open. A plain click on an empty queue adds the
  // type and closes immediately.
  function handleItemClick(event: React.MouseEvent, itemType: DescItemTypeRef) {
    const isQueueMode = event.ctrlKey || event.metaKey || queuedItemTypes.length > 0;
    if (isQueueMode) {
      toggleQueued(itemType);
    } else {
      addAndClose(itemType);
    }
  }

  // Space toggles the queued selection (like a checkbox); Enter falls through to the button's click
  // (add and close).
  function handleItemKeyDown(event: React.KeyboardEvent, itemType: DescItemTypeRef) {
    if (event.key === " ") {
      event.preventDefault();
      toggleQueued(itemType);
    }
  }

  // Index of the group a type belongs to, in the group order used by the form (buildGroupsForm).
  // Types without a group sort last so the composite (group, viewOrder) order matches the rendered form.
  function groupIndexOfType(typeId: number) {
    const index = descItemGroups.ids.findIndex((groupId) =>
      descItemGroups[groupId].itemTypes.some((itemType) => itemType.id === typeId),
    );
    return index === -1 ? descItemGroups.ids.length : index;
  }

  // Submit in the final form order (group order, then viewOrder within a group) so the auto-focus,
  // which targets the first submitted type, lands on the topmost new field.
  function sortByFormOrder(types: DescItemTypeRef[]) {
    return [...types].sort((a, b) => {
      const groupDiff = groupIndexOfType(a.id) - groupIndexOfType(b.id);
      return groupDiff !== 0 ? groupDiff : a.viewOrder - b.viewOrder;
    });
  }

  function handleSubmitChecked() {
    const types = sortByFormOrder(queuedItemTypes);
    if (types.length === 0) {
      return;
    }
    onSubmit(types);
    onClose();
  }

  // Render autocomplete rows via the default ListItem, marking queued types with a checkmark so
  // they stay visible and show their selected state instead of disappearing from the list.
  function renderItem(itemProps: { item?: DescItemTypeRef }) {
    const isQueued = !!itemProps.item && queuedItemTypes.some(({ id }) => id === itemProps.item!.id);
    return (
      <ListItem
        {...itemProps}
        renderName={(item: DescItemTypeRef) => (
          <span className={styles.itemName}>
            <CheckmarkRegular className={mergeClasses(styles.queuedCheck, !isQueued && styles.queuedCheckHidden)} />
            {item.name}
          </span>
        )}
      />
    );
  }

  // The server omits impossible types from itemTypes (impossible is the default state, so it isn't
  // transferred). A type missing from itemTypes is therefore impossible for this node; in non-strict
  // mode it is still offered, but only when its code belongs to the rule set (node-compatible types).
  const modifiedItemTypes: Array<DescItemTypeRef & { className: string }> = descItemTypes
    .filter((item) => {
      const descItem = descItems.find(({ itemTypeId }) => itemTypeId === item.id);
      if (descItem) {
        return false;
      }
      const itemType = itemTypes.find(({ itemTypeId }) => itemTypeId === item.id);
      if (itemType && itemType.type !== MandatoryType.Impossible) {
        return true;
      }
      const isNodeCompatible = ruleSetItemTypeCodes?.includes(item.code) ?? false;
      return !strictMode && isNodeCompatible;
    })
    .map((item) => {
      const itemType = itemTypes.find(({ itemTypeId }) => itemTypeId === item.id);
      const type = itemType ? itemType.type : MandatoryType.Impossible;
      // The queued class is part of className (which ListItem compares in shouldComponentUpdate)
      // so toggling the queue reliably re-renders the row and updates its checkmark.
      const isQueued = queuedItemTypes.some(({ id }) => id === item.id);
      const className = `type-${type.toLowerCase()}${isQueued ? " queued" : ""}`;
      return { ...item, className };
    });

  return (
    <ModalDialogWrapper className="dialog-lg" title={formatMessage(messages.addDescItemFormTitle)} onHide={onClose}>
      <Form onSubmit={(e) => {
        e.preventDefault();
        handleSubmitChecked();
      }}>
        <Modal.Body>
          <Text
            size={200}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "4px",
              marginBottom: "8px",
              color: tokens.colorNeutralForeground3,
            }}
          >
            <InfoRegular />
            {formatMessage(messages.multiAddHint)}
          </Text>
          {/*
            TODO: replace ItemTypeField (legacy Autocomplete) with a Fluent UI Combobox.
            Also drop the autocomplete-desc-item-type wrapper and move option styling (the
            type-impossible grey/italic rows) into the Combobox via makeStyles. That frees this
            form from AddDescItemTypeForm.scss, which it currently only borrows via the legacy
            form's global import.
          */}
          <div className="autocomplete-desc-item-type">
            <ItemTypeField
              ref={inputRef}
              descItemTypes={modifiedItemTypes}
              // @ts-expect-error ItemTypeField's Props omit onChange/value/renderItem, which it forwards to Autocomplete.
              onChange={handleChange}
              value={selectedItemType}
              renderItem={renderItem}
            />
          </div>
          <div>
            {getPossibleItemTypes().map((node, index) => {
              return (
                <FormGroup key={index}>
                  <FormLabel className={"d-block"}>{node.name}</FormLabel>
                  {node.children.map((itemType) => {
                    const isQueued = queuedItemTypes.some(({ id }) => id === itemType.id);
                    const serverType = itemTypes.find(({ itemTypeId }) => itemTypeId === itemType.id);
                    const isImpossible = !serverType || serverType.type === MandatoryType.Impossible;
                    return (
                      <Button
                        variant="link"
                        className={mergeClasses(
                          styles.addLink,
                          isQueued && styles.queued,
                          isQueued && isImpossible && styles.queuedImpossible,
                        )}
                        key={itemType.id}
                        onClick={(e) => handleItemClick(e, itemType)}
                        onKeyDown={(e) => handleItemKeyDown(e, itemType)}
                      >
                        {itemType.name}
                        {isQueued && <DismissRegular className={styles.dismissIcon} />}
                      </Button>
                    );
                  })}
                </FormGroup>
              );
            })}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            disabled={queuedItemTypes.length === 0}
            type={"submit"}
          >
            {i18n("global.action.add")}
          </Button>
          <Button variant="link" onClick={onClose}>
            {i18n("global.action.cancel")}
          </Button>
        </Modal.Footer>
      </Form>
    </ModalDialogWrapper>
  );
}
