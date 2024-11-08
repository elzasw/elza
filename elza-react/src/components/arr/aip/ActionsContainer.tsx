import { Icon } from "../../../components/shared";
import i18n from "../../../components/i18n";
import { Button } from "../../../components/ui";
import { AREA_AIPS, AREA_SELECTED_AIPS } from "../../../actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "../../../shared/utils";
import { useThunkDispatch } from "utils/hooks";
import { modalDialogShow } from "actions/global/modalDialog";
import AipAssignmentModal from "./assignment/AipAssignmentModal";
import AipIndividualAssignmentModal from "./assignment/AipIndividualAssignmentModal.tsx";
import {AppState} from "../../../typings/store";
import * as aipActions from "../../../actions/aip/aip.ts";


type ActionsContainerProps = {
    fund: any,
    readMode: boolean
}

const ActionsContainer = ({fund, readMode}: ActionsContainerProps) => {
    const selectedAips = useSelector((state: any) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aips = useSelector((state: any) => storeFromArea(state, AREA_AIPS));
    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP))
    const dispatch = useThunkDispatch();

    const handleConnectIndividually = () => {
        if (selectedAips.count == 1) {
            dispatch(aipActions.selectAip(selectedAips.rows[0].aipId));
        }

        if (aip) {
            dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.aip.assignment.individually.title'),
                    <AipIndividualAssignmentModal aipId={aip.id} tree={fund.fundTree}/>,
                    "aip-assignment"
                ),
            );
        }
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
        <div className="aip-actions-container">
            <Button onClick={handleConnectBulk} disabled={readMode}>
                <Icon glyph="fa-solif fa-link" />
                <div>{i18n('arr.aip.assignment.bulk')} ({selectedAips.count > 0 ? selectedAips.count : aips.count})</div>
            </Button>
            <Button onClick={handleConnectIndividually} disabled={readMode}>
                <Icon glyph="fa-solif fa-link" />
                <div>{i18n('arr.aip.assignment.individually')}</div>
            </Button>
        </div>
    );
}

export default ActionsContainer;
