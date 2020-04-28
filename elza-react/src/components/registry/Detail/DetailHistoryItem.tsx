import React from 'react';
import {Row, Col} from 'react-bootstrap';
import {AeState, AeStateHistoryVO} from "../../../api/generated/model";
//import {formatDateTime} from "../../dateutils";
import {getLabel} from "../../../api/AeStateEnumInfo";

type Props = {
  historyItem: AeStateHistoryVO
}

const DetailHistoryItem: React.FC<Props> = props => (
  <div className="p-2 history-item">
    <h4>{getLabel(props.historyItem.state)}</h4>
    <Row justify="space-between">
      <Col>{props.historyItem.user.displayName}</Col>
      <Col>TODO FORMAT {props.historyItem.change}</Col>
    </Row>
    <div className="mt-1">
      {props.historyItem.comment}
    </div>
  </div>
);

export default DetailHistoryItem;
