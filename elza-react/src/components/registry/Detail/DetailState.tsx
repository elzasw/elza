import React from 'react';
import {Col, Row} from 'react-bootstrap';
import {ApState} from "../../../api/generated/model";
import * as AeStateEnumInfo from "../../../api/old/ApStateEnumInfo";
import "./DetailState.scss";
import {Icon} from "../../index";

interface Props {
  state: ApState;
}

/**
 * Zobrazí stav AE včetně ikony.
 */
const RecordDetailState: React.FC<Props> = ({state}) => (
  <Row className="detail-state justify-content-between align-middle">
    <Col className="detail-state-icon" style={{backgroundColor: AeStateEnumInfo.getColor(state)}}>
      <Icon
        glyph={AeStateEnumInfo.getIcon(state)}
      />
    </Col>
    <Col style={{marginLeft: '2px'}}>
      {AeStateEnumInfo.getLabel(state)}
    </Col>
  </Row>
);

export default RecordDetailState;
