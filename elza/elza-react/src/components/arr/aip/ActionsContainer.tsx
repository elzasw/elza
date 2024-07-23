import { Icon } from "../../../components/shared";
import i18n from "../../../components/i18n";
import { Button } from "../../../components/ui";
import { AREA_AIPS, AREA_SELECTED_AIPS } from "../../../actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "../../../shared/utils";


const ActionsContainer = () => {
    const data = useSelector((state: any) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aips = useSelector((state: any) => storeFromArea(state, AREA_AIPS));

    const handleConnectIndividually = () => {

    }

    const handleConnectCollectively = () => {
        
    }


    return (
        <div className="ab-actions-container">
            <Button onClick={handleConnectCollectively}>
                <Icon glyph="fa-solif fa-link" />
                <div>{i18n('arr.ab.connect.collectively')} ({data.count > 0 ? data.count : aips.count})</div>
            </Button>
            <Button onClick={handleConnectIndividually}>
                <Icon glyph="fa-solif fa-link" />
                <div>{i18n('arr.ab.connect.individually')}</div>
            </Button>
        </div>
    );
}

export default ActionsContainer;