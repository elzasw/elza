import React from 'react';
import {Col, Row} from 'react-bootstrap';
import "./DetailState.scss";
import {Icon} from "../../index";
import {StateApproval, StateApprovalCaption, StateApprovalColor, StateApprovalIcon} from "../../../api/StateApproval";

interface Props {
    state: StateApproval;
}

/**
 * Zobrazí stav AE včetně ikony.
 */
const DetailState: React.FC<Props> = ({state}) => (<div>
    <Row className="detail-state justify-content-between align-middle ml-0">
        <Col className="detail-state-icon" style={{backgroundColor: StateApprovalColor(state)}}>
            <Icon
                glyph={StateApprovalIcon(state)}
            />
        </Col>
        <Col style={{marginLeft: '2px'}}>
            {StateApprovalCaption(state)}
        </Col>
    </Row></div>
);

export default DetailState;
