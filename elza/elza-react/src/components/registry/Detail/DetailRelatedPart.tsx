import React, {FC, useEffect, useState} from 'react';
import {Col, Row} from 'react-bootstrap';
import DetailItem from './DetailItem';
//import EditModal from '../EditModal';
import classNames from "classnames";
import "./DetailRelatedPart.scss";
//import {faArrowRight, faEdit, faTrash} from "@fortawesome/free-solid-svg-icons";
//import DetailActionButton from "../DetailActionButton";
//import {CodelistData} from "../../shared/reducers/codelist/CodelistTypes";
import {connect} from "react-redux";
import * as PartTypeInfo from "../../../api/old/PartTypeInfo";
import DetailMultipleItem from "./DetailMultipleItem";
import Icon from '../../shared/icon/Icon';
import {MOCK_CODE_DATA} from './mock';
import {ApPartVO} from "../../../api/ApPartVO";
import {ApValidationErrorsVO} from "../../../api/ApValidationErrorsVO";
import {ApItemVO} from "../../../api/ApItemVO";
import {PartValidationErrorsVO} from "../../../api/PartValidationErrorsVO";
import ValidationResultIcon from "../../ValidationResultIcon";
import {Bindings} from "../../../types";
//import {sortItems} from "../../itemutils";
//import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
//import ValidationResultIcon from "../ValidationResultIcon";

interface Props {
  label: string;
  part: ApPartVO;
  globalCollapsed: boolean;
  onDelete?: (part: ApPartVO) => void;
  onEdit?: (part: ApPartVO) => void;
  editMode?: boolean;
  codelist: any;
  globalEntity: boolean;
  partValidationError?: PartValidationErrorsVO;
  bindings: Bindings;
}

const DetailRelatedPart: FC<Props> = ({label, part,globalEntity, editMode, onDelete, onEdit, globalCollapsed, codelist, partValidationError, bindings}) => {
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

  const renderItems = (items: Array<ApItemVO>) => {
    if (items.length === 0) {
      return <Col className={"mt-1"}><i>Nejsou definovány žádné hodnoty atributů</i></Col>;
    }

    let result: any = [];

    let index=0;
    while (index < items.length) {
      let index2 = index + 1;
      while (index2 < items.length && items[index].typeId === items[index2].typeId) {
        index2++;
      }

      let itemInfo;
      if (codelist.partItemTypeInfoMap[partType]) {
        itemInfo = codelist.partItemTypeInfoMap[partType][items[index].typeId];
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

  const sortedItems =  part.items // sortItems(partType, part.items, codelist);

  const showValidationError = () => {
    if (editMode && partValidationError && partValidationError.errors && partValidationError.errors.length > 0) {
      return <ValidationResultIcon message={partValidationError.errors} />
    }
  };

  return <div className="detail-related-part">
    <Row className={classNameHeader + " align-items-center"}>
      <Col>
        <Icon glyph='fa-arrow-right' fixedWidth className="detail-related-part-icon"/>
      </Col>
      <Col className="detail-part-label" onClick={() => setCollapsed(!collapsed)}
           title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}>
        {label || <i>Popis záznamu entity</i>}
      </Col>
      {editMode && <Col style={{flex: 1}} className="ml-2">
          <Row>{/* gutter={8} */}
              <Col>
                  <Icon
                    glyph={'fa-pencil'}
                    onClick={() => onEdit && onEdit(part)}
                  />
              </Col>
            <Col>
              <Icon
                glyph={'fa-trash'}
                onClick={() => onDelete && onDelete(part)}
              />
            </Col>
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

const mapStateToProps = ({codelist}: any) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(
  mapStateToProps
)(DetailRelatedPart);
