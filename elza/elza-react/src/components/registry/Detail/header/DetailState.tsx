import React from 'react';
import {Icon} from "../../../index";
import {StateApproval, StateApprovalEx, StateApprovalCaption, StateApprovalIcon} from "../../../../api/StateApproval";

interface Props {
    state: StateApproval | StateApprovalEx;
}

/**
 * Zobrazí stav AE včetně ikony.
 */
const DetailState: React.FC<Props> = ({state}) => (
    <div className="d-inline-block">
        {StateApprovalCaption(state)}
    </div>
);

export default DetailState;
