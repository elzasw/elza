import React from 'react';
import {Icon} from "../../../index";
import {RevStateApproval, RevStateApprovalCaption, RevStateApprovalIcon} from "../../../../api/RevStateApproval";

interface Props {
    state: RevStateApproval;
}

/**
 * Zobrazí stav AE včetně ikony.
 */
const DetailRevState: React.FC<Props> = ({state}) => (
    <div className="d-inline-block">
        <Icon glyph={RevStateApprovalIcon(state)} className={'mr-1'}/>
        {RevStateApprovalCaption(state)}
    </div>
);

export default DetailRevState;
