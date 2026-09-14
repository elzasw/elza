// --
import React, { useState, useEffect } from 'react';
import { connect, useSelector } from 'react-redux';
import { AbstractReactComponent, Icon, Tabs } from 'components/shared';
import { defineMessages, useIntl } from 'react-intl';
import { permissionScopeMessages } from './permissionMessages';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    tabsUsers: { id: 'admin.perms.fund.tabs.users', defaultMessage: 'Uživatelé' },
    tabsGroups: { id: 'admin.perms.fund.tabs.groups', defaultMessage: 'Skupiny' },
    fundTitle: { id: 'admin.fund.title', defaultMessage: 'Archivní soubor' },
});
import AdminRightsContainer from './AdminRightsContainer';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as fundActions from '../../actions/admin/fund';
import { HorizontalLoader } from '../shared/index';
import FundUsersPanel from './FundUsersPanel';
import FundGroupsPanel from './FundGroupsPanel';
import FundsPermissionPanel from './FundsPermissionPanel';
import DetailHeader from '../shared/detail/DetailHeader';
import './FundDetail.scss';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { ALL_ID } from 'actions/admin/adminPermissions';

enum FundDetailTabs {
    TAB_USERS = 0,
    TAB_GROUPS = 1,
}

interface SelectedItem {
    id: number | string | null;
    index: number | null;
}



export function FundDetailFn() {
    const intl = useIntl();
    // Skládá se při renderu, aby popisky reagovaly na přepnutí jazyka.
    const tabItems = [
        { id: FundDetailTabs.TAB_USERS, title: intl.formatMessage(messages.tabsUsers) },
        { id: FundDetailTabs.TAB_GROUPS, title: intl.formatMessage(messages.tabsGroups) },
    ];
    const [selectedUser, setSelectedUser] = useState<SelectedItem>();
    const [selectedGroup, setSelectedGroup] = useState<SelectedItem>();
    const [selectedTab, setSelectedTab] = useState(tabItems[FundDetailTabs.TAB_USERS]);

    const fund = useSelector((state: AppState) => storeFromArea(state, fundActions.AREA_ADMIN_FUND))
    const dispatch = useThunkDispatch();

    function fetchData() {
        if (fund.id !== ALL_ID) {
            dispatch(fundActions.fundFetchIfNeeded(fund.id));
        } else {
            if (!fund.data || fund.data.id !== ALL_ID) {
                dispatch(
                    fundActions.setFund({
                        id: ALL_ID,
                        name: intl.formatMessage(permissionScopeMessages.fundAll),
                    }),
                );
            }
        }
    };

    function renderTabContent() {
        console.log("#### render tab content", selectedUser, selectedGroup, selectedTab);
        switch (selectedTab.id) {
            case FundDetailTabs.TAB_USERS:
                return (
                    <FundUsersPanel
                        fundId={fund.id}
                        onSelectItem={(item: { id: number }, index: number) => {
                            console.log("#### select user item", item, index);
                            // this.selectedUser = { index, id: item.id };
                            setSelectedUser({ index, id: item.id });
                        }}
                        selectedPermission={selectedUser || { id: undefined, index: 0 }}
                    />
                );
            case FundDetailTabs.TAB_GROUPS:
                return (
                    <FundGroupsPanel
                        fundId={fund?.id}
                        onSelectItem={(item: { id: number }, index: number) => {
                            console.log("#### select group item")
                            setSelectedGroup({ index, id: item.id });
                            // this.selectedGroup = { index, id: item.id };
                        }}
                        selectedPermission={selectedGroup || { id: undefined, index: 0 }}
                    />
                );
            default:
                return null;
        }
    };

    useEffect(() => {
        setSelectedUser(undefined);
        setSelectedGroup(undefined);

        console.log("#### fund changed", fund)
        fetchData()
    }, [fund.id])

    if (!fund.fetched || fund.isFetching) {
        return <HorizontalLoader />;
    }

    return (
        <AdminRightsContainer
            className="detail-container"
            header={
                <DetailHeader
                    icon={<Icon glyph="fa-group" />}
                    title={fund.data.name}
                    flagLeft={intl.formatMessage(messages.fundTitle)}
                    subtitle={fund.data.internalCode}
                />
            }
        >
            <div className="permissions-container">
                <Tabs.Container>
                    <Tabs.Tabs
                        asTabs
                        items={tabItems}
                        activeItem={selectedTab}
                        onSelect={(item: any) => setSelectedTab({ ...tabItems[Number(item)] })}
                    />
                    <Tabs.Content>{renderTabContent()}</Tabs.Content>
                </Tabs.Container>
            </div>
        </AdminRightsContainer>
    );
}

export default FundDetailFn;
