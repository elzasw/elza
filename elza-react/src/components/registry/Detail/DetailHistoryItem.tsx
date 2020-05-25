import React from 'react';
import {Col, Row} from 'react-bootstrap';
import {ApStateHistoryVO} from "../../../api/ApStateHistoryVO";
import {formatDate} from "../../validate";
import {StateApprovalCaption} from "../../../api/StateApproval";

type Props = {
    historyItem: ApStateHistoryVO
}

const DetailHistoryItem: React.FC<Props> = props => (
    <div className="mt-2 history-item">
        <h4>{StateApprovalCaption(props.historyItem.state)}</h4>
        <Row className="justify-space-between">
            <Col>{props.historyItem.username}</Col>
            <Col>{formatDate(new Date(props.historyItem.changeDate))}</Col>
        </Row>
        <div className="mt-1">
            {props.historyItem.comment}
        </div>
    </div>
);

export default DetailHistoryItem;
