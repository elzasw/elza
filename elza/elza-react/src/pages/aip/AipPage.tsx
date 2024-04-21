/**
 * Stránka pro správu aip.
 */
import React, { FC, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Ribbon } from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import { RibbonGroup } from 'components/shared';
import storeFromArea from '../../shared/utils/storeFromArea';
import { AREA_AIP, selectAip } from '../../actions/aip/aip';
import { AppState } from '../../typings/store';
import { useThunkDispatch } from 'utils/hooks';
import {urlAip} from '../../constants';
import { useHistory, useRouteMatch } from 'react-router';
import {AipList} from "../../components/aip/AipList.tsx";
import AipDetail from "../../components/aip/AipDetail.tsx";

interface AipPageUrlParams {
    id?: string;
}

export const AipPage: FC = () => {
    const dispatch = useThunkDispatch();
    const splitter = useSelector((state: AppState) => state.splitter);
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const history = useHistory();
    const match = useRouteMatch<AipPageUrlParams>();

    useEffect(() => {
        const id = match.params?.id;

        console.log("aip id " + id);
        console.log("aip id " + aip?.id);
        if (id != null) {
            dispatch(selectAip(id));
        } else if (aip?.id != null) {
            history.replace(urlAip(aip.id));
        }
    }, [match.params.id])

    const buildRibbon = () => {
        const altActions = [];
        const itemActions = [];

        let altSection: React.ReactNode;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt-actions" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }
        let itemSection: React.ReactNode;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item-actions" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return <Ribbon altSection={altSection} itemSection={itemSection} />;
    }


    return (
        <PageLayout
            splitter={splitter}
            className="aip-page"
            ribbon={buildRibbon()}
            leftPanel={<AipList activeAip={aip} />}
            centerPanel={aip.id !== null ? <AipDetail /> : undefined}
        />
    );
}

export default AipPage;
