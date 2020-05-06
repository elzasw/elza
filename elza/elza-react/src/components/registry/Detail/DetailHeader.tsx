import React, {FC} from 'react';
import RecordDetailState from './DetailState';
import {Button, Col, Row} from 'react-bootstrap';
import {AeDetailHeadVO} from '../../../api/generated/model';
import DetailDescriptions from "./DetailDescriptions";
import DetailDescriptionsItem from "./DetailDescriptionsItem";
//import {CodelistData} from "../../shared/reducers/codelist/CodelistTypes";
import {connect} from "react-redux";
import Icon from '../../shared/icon/Icon';
//import {BASE_API_URL} from "../../constants";
//import {faAngleDoubleDown, faAngleDoubleUp, faExclamationTriangle} from "@fortawesome/free-solid-svg-icons";
//import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
//import {formatDate, formatDateTime} from "../../dateutils";
//import ValidationResultIcon from "../ValidationResultIcon";
import ArchiveEntityName from "./ArchiveEntityName";
import {MOCK_CODE_DATA} from './mock';
import {ApValidationErrorsVO} from "../../../api/ApValidationErrorsVO";

interface Props {
  head: AeDetailHeadVO;
  codelist: any;
  onToggleCollapsed?: () => void;
  collapsed: boolean;
  id?: number;
  validationResult?: ApValidationErrorsVO;
}
const formatDate = (a: any, ...other) => a;
const formatDateTime = (a: any, ...other) => a;

const DetailHeader: FC<Props> = ({head, codelist, id, collapsed, onToggleCollapsed, validationResult}) => {
  const aeType = codelist.aeTypesMap[head.aeTypeId];

  const showValidationError = () => {
    if (validationResult && validationResult.errors && validationResult.errors.length > 0) {
      return <Col>
        ValidationResultIcon message={validationResult.errors}
        </Col>;
    }
  };

  return <Row className="mt-2 ml-3 mr-3 pb-2 brb-3 space-between middle">
    <Col className={"pr-2"}>
      <img height={75} width={75} src={`/client/code/ae-type/${aeType.code}/icon`} />
    </Col>
    <Col style={{flex: 1}}>
      <h1><ArchiveEntityName name={head.name} description={head.description}/> {showValidationError()}</h1>
      <h3>{aeType.name}</h3>
      <DetailDescriptions className="mt-1">
        <DetailDescriptionsItem><RecordDetailState state={head.aeState}/></DetailDescriptionsItem>
        {id && <DetailDescriptionsItem label="ID:">{id}</DetailDescriptionsItem>}
        {head.lastChange && <DetailDescriptionsItem label="Upravil:">
          {head.lastChange.user.displayName} <span
          title={"Upraveno " + formatDateTime(head.lastChange.change, {})}>({formatDate(head.lastChange.change)})</span>
        </DetailDescriptionsItem>}
      </DetailDescriptions>
    </Col>
    <Col>
      <Button onClick={onToggleCollapsed} size="sm" title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}>
        <Icon className="icon" glyph={collapsed ? 'fa-angle-double-down' : 'fa-angle-double-up'}/>
      </Button>
    </Col>
  </Row>
};

const mapStateToProps = ({codelist}: any) => ({
    codelist: MOCK_CODE_DATA
});

export default connect(
  mapStateToProps
)(DetailHeader);
