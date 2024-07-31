import { Icon } from "../../../components/shared";
import i18n from "../../../components/i18n";
import { Button } from "../../../components/ui";
import { AREA_AIPS, AREA_SELECTED_AIPS } from "../../../actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "../../../shared/utils";
import { useThunkDispatch } from "utils/hooks";
import { modalDialogShow } from "actions/global/modalDialog";
import AipAssignmentModal from "./assignment/AipAssignmentModal";
import { useEffect } from "react";


type ActionsContainerProps = {
    fund: any
}

const ActionsContainer = ({fund}: ActionsContainerProps) => {
    const selectedAips = useSelector((state: any) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aips = useSelector((state: any) => storeFromArea(state, AREA_AIPS));
    console.log('aips :>> ', selectedAips);
    const dispatch = useThunkDispatch();

    const handleConnectIndividually = () => {

    }

    const handleConnectBulk = () => {
        dispatch(
            modalDialogShow(
                this,
                i18n('arr.aip.assignment.bulk.title'),
                <AipAssignmentModal aips={selectedAips.count > 0 ? selectedAips.rows : aips.rows} tree={fund.fundTree}/>,
                "aip-assignment"
            ),
        );
    }

    return (
        <div className="actions-container">
            <Button onClick={handleConnectBulk}>
                <Icon glyph="fa-solif fa-link" />
                <div>{i18n('arr.aip.assignment.bulk')} ({selectedAips.count > 0 ? selectedAips.count : aips.count})</div>
            </Button>
            <Button onClick={handleConnectIndividually}>
                <Icon glyph="fa-solif fa-link" />
                <div>{i18n('arr.aip.assignment.individually')}</div>
            </Button>
        </div>
    );
}

export default ActionsContainer;
