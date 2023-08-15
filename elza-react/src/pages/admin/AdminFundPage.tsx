/**
 * Stránka pro správu uživatelů.
 */
import React, { FC, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Ribbon } from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import { RibbonGroup } from 'components/shared';
import storeFromArea from '../../shared/utils/storeFromArea';
import { AREA_ADMIN_FUND, selectFund } from '../../actions/admin/fund';
import FundDetail from '../../components/admin/FundDetail';
import { FundList } from '../../components/admin/funds';
import { AppState } from '../../typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { urlAdminFund } from '../../constants';
import { useHistory, useRouteMatch } from 'react-router';

interface AdminFundPageUrlParams {
    id?: string;
}

export const AdminFundPage: FC = () => {
    const dispatch = useThunkDispatch();
    const splitter = useSelector((state: AppState) => state.splitter);
    const fund = useSelector((state: AppState) => storeFromArea(state, AREA_ADMIN_FUND));
    const history = useHistory();
    const match = useRouteMatch<AdminFundPageUrlParams>();

    useEffect(() => {
        const id = match.params?.id;

        if (id != null) {
            dispatch(selectFund(id));
        } else if (fund?.id != null) {
            history.replace(urlAdminFund(fund.id));
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

        return <Ribbon admin altSection={altSection} itemSection={itemSection} />;
    }


    return (
        <PageLayout
            splitter={splitter}
            className="admin-fund-page"
            ribbon={buildRibbon()}
            leftPanel={<FundList activeFund={fund} />}
            centerPanel={fund.id !== null ? <FundDetail /> : undefined}
        />
    );
}

export default AdminFundPage;
