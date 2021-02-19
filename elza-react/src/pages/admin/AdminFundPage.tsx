/**
 * Stránka pro správu uživatelů.
 */
import React, {FC} from 'react';
import {useSelector} from 'react-redux';
import {Ribbon} from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import { RibbonGroup } from 'components/shared';
import storeFromArea from '../../shared/utils/storeFromArea';
import {AREA_ADMIN_FUND} from '../../actions/admin/fund';
import FundDetail from '../../components/admin/FundDetail';
import { FundList } from '../../components/admin/funds';
import { AppState } from '../../typings/store';

export const AdminFundPage:FC = () => {
    const splitter = useSelector((state:AppState) => state.splitter);
    const fund = useSelector((state:AppState) => storeFromArea(state, AREA_ADMIN_FUND));

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

        return <Ribbon admin altSection={altSection} itemSection={itemSection} />;
    }


    return (
        <PageLayout
            splitter={splitter}
            className="admin-fund-page"
            ribbon={buildRibbon()}
            leftPanel={<FundList activeFund={fund}/>}
            centerPanel={fund.id !== null ? <FundDetail/> : undefined}
        />
    );
}

export default AdminFundPage;
