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
import { FormItemType, MandatoryType, NodeItem } from "elza-api";
import { RulDescItemTypeVO } from "api/RulDescItemTypeVO";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import {
  AddRegular,
} from "@fluentui/react-icons";
// import { Combobox, makeStyles, Option } from "@fluentui/react-components";
import { getOneSettings } from "../ArrUtils";
import { useInitialFocus } from "../search-funds-form/filters/utils";

/**
 * Formulář přidání nové desc item type.
 */
interface Props {
  // groups: ViewDescItemGroups[];
  itemTypes: FormItemType[];
  descItems: NodeItem[];
  onSubmit: (descItemType: DescItemTypeRef) => void;
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

export function AddDescItemTypeForm({ itemTypes, descItems, onSubmit, onClose }: Props) {
  const [selectedItemType, setSelectedItem] = useState<DescItemTypeRef>();
  const descItemTypes = useAppSelector(({ refTables }) => refTables.descItemTypes.items);
  const descItemGroups = useAppSelector(({ refTables }) => refTables.groups.data);
  // const activeFund = useAppSelector(({arrRegion}) => arrRegion.funds[arrRegion.activeIndex]);
  const strictMode:boolean = useAppSelector(({userDetail, arrRegion}) => {
    const activeFund = arrRegion.funds[arrRegion.activeIndex];
    const strictModeSetting = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', activeFund.id);
    return strictModeSetting ? JSON.parse(strictModeSetting.value) : true;
  })
  
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
    console.log("#adit - handleChange", itemType);
    setSelectedItem(itemType);
  }

  function handleSubmit(itemType: DescItemTypeRef = selectedItemType) {
    onSubmit(itemType);
    // onSubmit();
  }
  
  const modifiedItemTypes: Array<DescItemTypeRef & {className: string}> = descItemTypes.filter((item) => {
    const itemType = itemTypes.find(({itemTypeId}) => itemTypeId === item.id);
    const descItem = descItems.find(({itemTypeId}) => itemTypeId === item.id);
    // hide when descItem already added
    // and if impossible when strictMode enabled
    return !descItem && !(strictMode && itemType.type === MandatoryType.Impossible);
  }).map((item) => {
    const itemType = itemTypes.find(({itemTypeId}) => itemTypeId === item.id);
    return {
    ...item, 
    className: `type-${itemType.type.toLowerCase()}`
  }});

  return (
    <ModalDialogWrapper className="dialog-lg" title={"Pridat prvek popisu"} onHide={onClose}>
      <Form onSubmit={(e) => {
        e.preventDefault();
        handleSubmit(selectedItemType);
      }}>
        <Modal.Body>
          <div>
            {getPossibleItemTypes().map((node, index) => {
              return (
                <FormGroup key={index}>
                  <FormLabel className={"d-block"}>{node.name}</FormLabel>
                  {node.children.map((itemType) => (
                    <Button className="add-link" key={itemType.id} onClick={() => onSubmit(itemType)}>
                      {/* <Icon glyph="fa-plus" /> */}
                      {itemType.name}
                    </Button>
                  ))}
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
          <Button variant="outline-secondary" disabled={!selectedItemType} type={"submit"}>
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
