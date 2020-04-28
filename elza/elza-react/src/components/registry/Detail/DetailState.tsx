import React from 'react';
import {Col, Row} from 'react-bootstrap';
import {AeState} from "../../../api/generated/model";
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as AeStateEnumInfo from "./../../api/AeStateEnumInfo";
import "./DetailState.scss";

interface Props {
  state: AeState;
}

/**
 * Zobrazí stav AE včetně ikony.
 */
const RecordDetailState: React.FC<Props> = ({state}) => (
  <Row  justify="space-between" align="middle" className="detail-state">
    <Col className="detail-state-icon" style={{backgroundColor: AeStateEnumInfo.getColor(state)}}>
      <FontAwesomeIcon
        className="icon"
        fixedWidth
        icon={AeStateEnumInfo.getIcon(state)}
      />
    </Col>
    <Col style={{marginLeft: '2px'}}>
      {AeStateEnumInfo.getLabel(state)}
    </Col>
  </Row>
);

export default RecordDetailState;
