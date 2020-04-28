import React, {FC, useEffect, useState} from 'react';
import {Col, Row} from 'react-bootstrap';
import {AeItemVO, AePartNameVO, AePartVO, AeValidationErrorsVO} from '../../../api/generated/model';
import DetailItem from './DetailItem';
//import EditModal from '../EditModal';
import classNames from "classnames";
import "./DetailPart.scss";
import {AePartNameClass} from "../../../api/AePartInfo";
//import DetailActionButton from "../DetailActionButton";
//import {CodelistData} from "../../shared/reducers/codelist/CodelistTypes";
import {connect} from "react-redux";
import * as PartTypeInfo from "../../../api/PartTypeInfo";
import DetailMultipleItem from "./DetailMultipleItem";
import Icon from '../../shared/icon/Icon';
import { MOCK_CODE_DATA } from './mock';
//import {sortItems} from "../../itemutils";
//import ValidationResultIcon from "../ValidationResultIcon";

interface Props {
  label: string;
  part: AePartVO;
  globalCollapsed: boolean;
  preferred?: boolean;
  onSetPreferred?: (part: AePartNameVO) => void;
  onDelete?: (part: AePartVO) => void;
  onEdit?: (part: AePartVO) => void;
  onAddRelated?: (part: AePartVO) => void;
  editMode?: boolean;
  singlePart: boolean;
  codelist: any;
  globalEntity: boolean;
  validationResult?: AeValidationErrorsVO;
}

const DetailPart: FC<Props> = ({label, part, editMode, onSetPreferred, singlePart, onDelete, onEdit, globalCollapsed, preferred, codelist, onAddRelated,globalEntity, validationResult}) => {
  const [collapsed, setCollapsed] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const partType = PartTypeInfo.getPartType(part["@class"]);

  useEffect(() => {
    setCollapsed(globalCollapsed);
  }, [globalCollapsed]);

  const classNameHeader = classNames(
    "detail-part",
    "detail-part-header",
    {
      "pb-1": collapsed,
      "detail-part-preferred": preferred,
      "detail-part-expanded": !collapsed
    }
  );

  // Rozbalený content
  const classNameContent = classNames(
    "detail-part mb-3",
    {
      "detail-part-preferred": preferred,
      "detail-part-expanded": !collapsed
    }
  );

  let showPreferredSwitch = false;
  if (part["@class"] === AePartNameClass) {
    showPreferredSwitch = !singlePart;
  }

  const renderItems = (items: Array<AeItemVO>) => {
    if (items.length === 0) {
      return <Col className={"mt-1"}><i>Nejsou definovány žádné hodnoty atributů</i></Col>;
    }

    let result: any = [];

    let index = 0;
    while (index < items.length) {
      let index2 = index + 1;
      while (index2 < items.length && items[index].itemTypeId === items[index2].itemTypeId) {
        index2++;
      }

      let itemInfo;
      if (codelist.partItemTypeInfoMap[partType]) {
        itemInfo = codelist.partItemTypeInfoMap[partType][items[index].itemTypeId];
      }
      let width = itemInfo ? itemInfo.width : 2;

      let sameItems = items.slice(index, index2);
      index = index2;

      let rows: any = [];
      if (sameItems.length > 1) {
        rows.push(<DetailMultipleItem key={index} items={sameItems} globalEntity={globalEntity}/>);
      } else {
        rows.push(<DetailItem key={index} item={sameItems[0]} globalEntity={globalEntity}/>);
      }

      result.push(<Col key={index}>{/* span={width <= 0 ? 24 : width * 2} */}
        {rows}
      </Col>);
    }

    return result;
  };

  const sortedItems = part.items //sortItems(partType, part.items, codelist);

  const showValidationError = () => {
    if (validationResult && validationResult.partErrors && validationResult.partErrors.length > 0) {
      const index = validationResult.partErrors.findIndex(value => value.id === part.id);
      if (index >= 0) {
        const errors = validationResult.partErrors[index].errors;
        if (errors && errors.length > 0) {
          return <Col>
              ValidationResultIcon
            {validationResult.partErrors[index].errors}
          </Col>;
        }
      }
    }
  };

  return <div className="detail-part">
    <Row
      className={classNameHeader + " align-items-center"}
    >
      <Col className="detail-part-label" onClick={() => setCollapsed(!collapsed)}
           title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}>
        {label || <i>Popis záznamu entity</i>}
        {preferred && <span className="detail-part-label-alt"> (preferované)</span>}
      </Col>
      {editMode && <Col style={{flex: 1}} className="ml-2">
        <Row>{/* gutter=8 */}
          {showPreferredSwitch && !preferred && <Col>
            <Icon
              glyph={'fa-star'}
              onClick={() => onSetPreferred && onSetPreferred((part as AePartNameVO))}
              style={{visibility: preferred ? "hidden" : "inherit"}}
            />
          </Col>}
          <Col>
            <Icon
              glyph={'fa-pencil'}
              onClick={() => onEdit && onEdit(part)}
            />
          </Col>
          {!preferred && <Col>
            <Icon
              glyph={'fa-trash'}
              onClick={() => onDelete && onDelete(part)}
            />
          </Col>}
          {onAddRelated &&
          <Col>
            <Icon
              glyph={'fa-plus'}
              onClick={() => onAddRelated(part)}
            />
          </Col>}
          {showValidationError()}
        </Row>
      </Col>}
      {!editMode && <Col style={{flex: 1}} className="ml-2">
          {showValidationError()}
      </Col>}
    </Row>

    {!collapsed && <div className={classNameContent}>
      <Row>
        {renderItems(sortedItems)}
      </Row>

        {/*<EditModal
        part={part}
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
      />*/}
    </div>}
  </div>
};

const mapStateToProps = ({codelist, app}: any) => ({
  codelist: MOCK_CODE_DATA,
  //validationResult: app[DETAIL_VALIDATION_RESULT].data,
});

export default connect(
  mapStateToProps
)(DetailPart);
