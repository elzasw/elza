import PropTypes from "prop-types";
import React, { useRef, useState } from "react";
import { FieldArray, reduxForm } from "redux-form";
import { AbstractReactComponent, i18n, Icon, ModalDialogWrapper } from "components/shared";
import { Form, FormGroup, FormLabel, Modal, Button } from "react-bootstrap";
//import {Button} from '../../ui';
import { submitForm } from "components/form/FormUtils.jsx";
// import "../nodeForm/AddDescItemTypeForm.scss";
import { ItemTypeField } from "components/arr/nodeForm/ItemTypeField";
import FF from "../../shared/form/FF";
import { getInfoSpecType } from "../../../stores/app/accesspoint/itemFormUtils";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { RulItemTypeType } from "api/RulItemTypeType";
import { FormItemType, MandatoryType } from "elza-api";
import { RulDescItemTypeVO } from "api/RulDescItemTypeVO";
import { EditItem } from "./types";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import {
  AddRegular,
} from "@fluentui/react-icons";
import { Text, tokens } from "@fluentui/react-components";
// import { Combobox, makeStyles, Option } from "@fluentui/react-components";
import { getOneSettings } from "../ArrUtils";
import { useInitialFocus } from "../search-funds-form/filters/utils";
import { defineMessages, useIntl } from "react-intl";

/**
 * Formulář přidání nové desc item type.
 */
interface Props {
  // groups: ViewDescItemGroups[];
  itemTypes: FormItemType[];
  descItems: EditItem[];
  onSubmit: (descItemTypes: DescItemTypeRef[]) => void;
  onClose: () => void;
}

// const useStyles = makeStyles({
//   [MandatoryType.Impossible]: {
//     /* Added in AddDescItemTypeForm.jsx:40 */
//     backgroundColor: "var(shade-2)",
//     color: "#888",
//     fontStyle: "italic",
//   },
//   [MandatoryType.Possible]: {
//     /* Added in AddDescItemTypeForm.jsx:40 */
//     backgroundColor: "transparent",
//   }
// });

export const messages = defineMessages({
  addDescItemFormTitle: {
    id: "add_desc_item_form_title",
    defaultMessage: "Přidat prvek popisu",
  },
  multiAddHint: {
    id: "add_desc_item_form_multi_hint",
    defaultMessage: "Tip: podržte Ctrl a klikněte pro přidání více prvků najednou.",
  },
});

export function AddDescItemTypeForm({ itemTypes, descItems, onSubmit, onClose }: Props) {
  const [selectedItemType, setSelectedItem] = useState<DescItemTypeRef>();
  const [queuedItemTypes, setQueuedItemTypes] = useState<DescItemTypeRef[]>([]);
  const descItemTypes = useAppSelector(({ refTables }) => refTables.descItemTypes.items);
  const descItemGroups = useAppSelector(({ refTables }) => refTables.groups.data);
  // const activeFund = useAppSelector(({arrRegion}) => arrRegion.funds[arrRegion.activeIndex]);
  const strictMode:boolean = useAppSelector(({userDetail, arrRegion}) => {
    const activeFund = arrRegion.funds[arrRegion.activeIndex];
    const strictModeSetting = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', activeFund.id);
    const strictModeValue = JSON.parse(strictModeSetting.value);
    return strictModeValue ?? true;
  })

  const { formatMessage } = useIntl();

  const inputRef = useRef(null);
  useInitialFocus(inputRef);

  // const styles = useStyles();

  function getPossibleItemTypes() {
    const possibleTypes = descItemTypes.filter(({ id }) => {
      const isAdded = !!descItems.find(({ itemTypeId }) => itemTypeId === id);
      if (isAdded) {
        return false;
      }
      const isPossible = !!itemTypes.find(
        ({ itemTypeId, type }) => itemTypeId === id && type === MandatoryType.Possible,
      );
      return isPossible;
    });
    const possibleGroups: Array<DescItemGroup & { children: DescItemTypeRef[] }> = [];
    descItemGroups.ids.map((groupId) => {
      const group = descItemGroups[groupId];
      const children = possibleTypes.filter(({ id }) => { return group.itemTypes.find((itemType) => itemType.id === id) });
      if (children.length > 0) {
        possibleGroups.push({ ...group, children })
      }
      // return {...group, children}
    })
    return possibleGroups;
  }

  function handleChange(itemType: DescItemTypeRef) {
    setSelectedItem(itemType);
  }

  function addAndClose(itemType: DescItemTypeRef) {
    onSubmit([itemType]);
    onClose();
  }

  // Ctrl/Cmd+click queues the type for a batch add and keeps the dialog open; a plain click adds it
  // and closes immediately.
  function handleItemClick(event: React.MouseEvent, itemType: DescItemTypeRef) {
    if (event.ctrlKey || event.metaKey) {
      setQueuedItemTypes((queued) =>
        queued.some(({ id }) => id === itemType.id)
          ? queued.filter(({ id }) => id !== itemType.id)
          : [...queued, itemType],
      );
    } else {
      addAndClose(itemType);
    }
  }

  function handleSubmitChecked() {
    const types = [...queuedItemTypes];
    if (selectedItemType && !types.some(({ id }) => id === selectedItemType.id)) {
      types.push(selectedItemType);
    }
    if (types.length === 0) {
      return;
    }
    onSubmit(types);
    onClose();
  }

  const modifiedItemTypes: Array<DescItemTypeRef & {className: string}> = descItemTypes.filter((item) => {
    const itemType = itemTypes.find(({itemTypeId}) => itemTypeId === item.id);
    const descItem = descItems.find(({itemTypeId}) => itemTypeId === item.id);
    // hide when descItem already added
    // and if impossible when strictMode enabled
    return !descItem && !!itemType && !(strictMode && itemType.type === MandatoryType.Impossible);
  }).map((item) => {
    const itemType = itemTypes.find(({itemTypeId}) => itemTypeId === item.id);
    return {
    ...item,
    className: `type-${itemType.type.toLowerCase()}`
  }});

  return (
    <ModalDialogWrapper className="dialog-lg" title={formatMessage(messages.addDescItemFormTitle)} onHide={onClose}>
      <Form onSubmit={(e) => {
        e.preventDefault();
        handleSubmitChecked();
      }}>
        <Modal.Body>
          <Text
            size={200}
            style={{ display: "block", marginBottom: "8px", color: tokens.colorNeutralForeground3 }}
          >
            {formatMessage(messages.multiAddHint)}
          </Text>
          <div>
            {getPossibleItemTypes().map((node, index) => {
              return (
                <FormGroup key={index}>
                  <FormLabel className={"d-block"}>{node.name}</FormLabel>
                  {node.children.map((itemType) => {
                    const isQueued = queuedItemTypes.some(({ id }) => id === itemType.id);
                    return (
                      <Button
                        className={`add-link${isQueued ? " queued" : ""}`}
                        key={itemType.id}
                        active={isQueued}
                        onClick={(e) => handleItemClick(e, itemType)}
                      >
                        {itemType.name}
                      </Button>
                    );
                  })}
                </FormGroup>
              );
            })}
          </div>
          <div className="autocomplete-desc-item-type">
          {/* <Combobox>
              {modifiedItemTypes.map(({className, ...item}) => {
                const itemType = itemTypes.find(({ itemTypeId }) => itemTypeId === item.id);
                return <Option
                  value={item.id.toString()}
                  className={styles[itemType.type]}
                >
                  {item.name}
                </Option>
              })}
          </Combobox> */}
            {/* @ts-ignore */}
            <ItemTypeField
              ref={inputRef}
              descItemTypes={modifiedItemTypes}
              /*@ts-ignore*/
              onChange={handleChange}
              value={selectedItemType}
            />
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            disabled={queuedItemTypes.length === 0 && !selectedItemType}
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
