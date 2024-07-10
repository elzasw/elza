/**
 * Stránka pro správu aip.
 */
import { FC, useEffect } from 'react';
import { useSelector } from 'react-redux';
import storeFromArea from '../../shared/utils/storeFromArea';
import { AREA_AIP, selectAip } from '../../actions/aip/aip';
import { AppState } from '../../typings/store';
import { useThunkDispatch } from 'utils/hooks';
import {urlAip} from '../../constants';
import { useHistory, useRouteMatch } from 'react-router';
import AipTable from 'components/aip/AipTable';
import './AipPage.scss';
import AipDetail from 'components/aip/AipDetail';
import AipPageRibbon from 'components/aip/AipPageRibbon';

interface AipPageUrlParams {
    id?: string;
}

export const AipPage: FC = () => {
    const dispatch = useThunkDispatch();
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const history = useHistory();
    const match = useRouteMatch<AipPageUrlParams>();

    useEffect(() => {
        const id = match.params?.id;

        if (id != null) {
            dispatch(selectAip(id));
        } else if (aip?.id != null) {
            history.replace(urlAip(aip.id));
        }
    }, [match.params.id]);

    return (
        <div className="aip-page">
            <AipPageRibbon />
            <div style={{display: "flex"}}>
                <div style={{flex: "1"}}>
                    <AipTable/>
                </div>
                <AipDetail />
            </div>
        </div>
    );
}

export default AipPage;
