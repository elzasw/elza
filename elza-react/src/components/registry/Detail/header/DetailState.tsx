import React from 'react';
import {Icon} from "../../../index";
import {StateApproval, StateApprovalCaption, StateApprovalIcon} from "../../../../api/StateApproval";

interface Props {
    state: StateApproval;
}

/**
 * Zobrazí stav AE včetně ikony.
 */
const DetailState: React.FC<Props> = ({state}) => (
    <div className="d-inline-block">
        <Icon glyph={StateApprovalIcon(state)} className={'mr-1'}/>
        {StateApprovalCaption(state)}
    </div>
);

export default DetailState;
