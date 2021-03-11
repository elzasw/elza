import React, {FC, useEffect, useState} from 'react';
import {Col, Row} from 'react-bootstrap';
import DetailItem from './DetailItem';
import classNames from "classnames";
import "./DetailRelatedPart.scss";
import {connect} from "react-redux";
import DetailMultipleItem from "./DetailMultipleItem";
import Icon from '../../shared/icon/Icon';
import {ApPartVO} from "../../../api/ApPartVO";
import {ApItemVO} from "../../../api/ApItemVO";
import {PartValidationErrorsVO} from "../../../api/PartValidationErrorsVO";
import ValidationResultIcon from "../../ValidationResultIcon";
import {Bindings} from "../../../types";
import {ItemType} from "../../../api/ApViewSettings";
import {objectById} from "../../../shared/utils";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {findViewItemType} from "../../../utils/ItemInfo";
import {RulPartTypeVO} from "../../../api/RulPartTypeVO";
import i18n from "../../i18n";

type Props = {
  label: string;
  part: ApPartVO;
  globalCollapsed: boolean;
  onDelete?: (part: ApPartVO) => void;
  onEdit?: (part: ApPartVO) => void;
  editMode?: boolean;
  globalEntity: boolean;
  partValidationError?: PartValidationErrorsVO;
  bindings: Bindings;
  itemTypeSettings: ItemType[];
} & ReturnType<typeof mapStateToProps>;

const DetailRelatedPart: FC<Props> = ({label, part,globalEntity, editMode, onDelete, onEdit, globalCollapsed, partValidationError, bindings, descItemTypesMap, itemTypeSettings, partTypesMap}) => {
  const [collapsed, setCollapsed] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const partType = partTypesMap[part.typeId];

  useEffect(() => {
    setCollapsed(globalCollapsed);
  }, [globalCollapsed]);

  const classNameHeader = classNames(
    "detail-part",
    "detail-part-header",
    {
      "pb-1": collapsed,
      "detail-part-expanded": !collapsed
    }
  );

  // Rozbalený content
  const classNameContent = classNames(
    "detail-part mb-3",
    {
      "detail-part-expanded": !collapsed
    }
  );

  const renderItems = (items: ApItemVO[]) => {
    if (items.length === 0) {
      return <Col className={"mt-1"}><i>Nejsou definovány žádné hodnoty atributů</i></Col>;
    }

    let result: any = [];

    let index = 0;
    while (index < items.length) {
      let index2 = index + 1;
      while (index2 < items.length && items[index].typeId === items[index2].typeId) {
        index2++;
      }

      const itemTypeExt: RulDescItemTypeExtVO = descItemTypesMap[items[index].typeId];
      let width = 2; // default
      if (itemTypeExt) {
        const itemType: ItemType | null = findViewItemType(itemTypeSettings, partType, itemTypeExt.code);
        if (itemType && itemType.width) {
          width = itemType.width;
        }
      }

      let sameItems = items.slice(index, index2);
      index = index2;

      let rows: any = [];
      if (sameItems.length > 1) {
        rows.push(<DetailMultipleItem key={index} items={sameItems} globalEntity={globalEntity} bindings={bindings}/>);
      } else {
        rows.push(<DetailItem key={index} item={sameItems[0]} globalEntity={globalEntity} bindings={bindings}/>);
      }

      result.push(<Col key={index} xs={width <= 0 ? 12 : width}>
        {rows}
      </Col>);
    }

    return result;
  };

  const sortedItems = part.items?part.items.sort((a, b) => {

    const aItemType: ItemType = objectById(itemTypeSettings, descItemTypesMap[a.typeId].code, 'code');
    const bItemType: ItemType = objectById(itemTypeSettings, descItemTypesMap[b.typeId].code, 'code');
    if (aItemType == null && bItemType == null) {
      return 0;
    } else if (aItemType == null) {
      return -1;
    } else if (bItemType == null) {
      return 1;
    } else {
      const aPos = aItemType.position || 9999;
      const bPos = bItemType.position || 9999;
      return aPos - bPos;
    }
  }):[];

  const showValidationError = () => {
    if (editMode && partValidationError && partValidationError.errors && partValidationError.errors.length > 0) {
      return <ValidationResultIcon message={partValidationError.errors} />
    }
  };

  const partBinding = bindings.partsMap[part.id];

  return <div className="detail-related-part ml-4 mb-2 pt-3">
    <Row className={classNameHeader + " align-items-center"}>
      <Col>
        <div
            className={'detail-part-label d-inline-block'}
            onClick={() => setCollapsed(!collapsed)}
            title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}
        >
                <span className={classNames('detail-part-label', 'mr-2', collapsed ? false : 'opened')}>
                    <Icon className="mr-2"
                        glyph={'fa-arrow-right'}
                    />
                    {label || <i>Popis záznamu entity</i>}
                </span>
        </div>

        {partBinding != null && <Icon glyph="fa-refresh" title={i18n('ap.binding.syncState.' + (partBinding ? 'SYNC_OK' : 'NOT_SYNCED'))} className={partBinding ? 'mr-2 ' : 'mr-2 disabled'} />}

        <Icon
            className={'mr-2 cursor-pointer'}
            glyph={'fa-pencil'}
            onClick={() => onEdit && onEdit(part)}
        />

        {showValidationError()}
      </Col>
    </Row>

    {!collapsed && <div className={classNameContent}>
        <Row>
          {renderItems(sortedItems)}
        </Row>
    </div>}
  </div>
};

const mapStateToProps = (state) => ({
    partTypesMap: state.refTables.partTypes.itemsMap as Record<number, RulPartTypeVO>,
    descItemTypesMap: state.refTables.descItemTypes.itemsMap as Record<number, RulDescItemTypeExtVO>,
});

export default connect(
  mapStateToProps
)(DetailRelatedPart);
