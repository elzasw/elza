// --
import React, { useState, useEffect } from 'react';
import { connect, useSelector } from 'react-redux';
import { AbstractReactComponent, i18n, Icon, Tabs } from 'components/shared';
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

enum FundDetailTabs {
    TAB_USERS = 0,
    TAB_GROUPS = 1,
}

interface SelectedItem {
    id: number | string | null;
    index: number | null;
}

const tabItems = [
    { id: FundDetailTabs.TAB_USERS, title: i18n('admin.perms.fund.tabs.users') },
    { id: FundDetailTabs.TAB_GROUPS, title: i18n('admin.perms.fund.tabs.groups') },
];

export function FundDetailFn() {
    const [selectedUser, setSelectedUser] = useState<SelectedItem>();
    const [selectedGroup, setSelectedGroup] = useState<SelectedItem>();
    const [selectedTab, setSelectedTab] = useState(tabItems[FundDetailTabs.TAB_USERS]);

    const fund = useSelector((state: AppState) => storeFromArea(state, fundActions.AREA_ADMIN_FUND))
    const dispatch = useThunkDispatch();

    function fetchData() {
        if (fund.id !== FundsPermissionPanel.ALL_ID) {
            dispatch(fundActions.fundFetchIfNeeded(fund.id));
        } else {
            if (!fund.data || fund.data.id !== FundsPermissionPanel.ALL_ID) {
                dispatch(
                    fundActions.setFund({
                        id: FundsPermissionPanel.ALL_ID,
                        name: i18n('admin.perms.tabs.funds.items.fundAll'),
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
                    flagLeft={i18n('admin.fund.title')}
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

// class FundDetail extends AbstractReactComponent {
//     static TAB_USERS = 0;
//     static TAB_GROUPS = 1;
//
//     static tabItems = [
//         { id: FundDetail.TAB_USERS, title: i18n('admin.perms.fund.tabs.users') },
//         { id: FundDetail.TAB_GROUPS, title: i18n('admin.perms.fund.tabs.groups') },
//     ];
//
//     /*
//      * Template for selected items
//      */
//     defaultSelectedItem = {
//         id: null,
//         index: 0,
//     };
//     /*
//      * Last selected fund item.
//      */
//     selectedUser = this.defaultSelectedItem;
//     /*
//      * Last selected scope item.
//      */
//     selectedGroup = this.defaultSelectedItem;
//
//     constructor(props) {
//         super(props);
//
//         this.state = {
//             selectedTabItem: FundDetail.TAB_USERS,
//         };
//     }
//
//     componentDidMount() {
//         this.fetchData(this.props);
//     }
//
//     UNSAFE_componentWillReceiveProps(nextProps) {
//         const fundId = this.props.fund.id;
//         const nextFundId = nextProps.fund.id;
//
//         // Reset selected permissions
//         if (fundId !== nextFundId) {
//             this.selectedUser = this.defaultSelectedItem;
//             this.selectedGroup = this.defaultSelectedItem;
//         }
//
//         this.fetchData(nextProps);
//     }
//
//     fetchData = props => {
//         const { fund } = props;
//         if (fund.id !== FundsPermissionPanel.ALL_ID) {
//             props.dispatch(fundActions.fundFetchIfNeeded(fund.id));
//         } else {
//             if (!fund.data || fund.data.id !== FundsPermissionPanel.ALL_ID) {
//                 props.dispatch(
//                     fundActions.setFund({
//                         id: FundsPermissionPanel.ALL_ID,
//                         name: i18n('admin.perms.tabs.funds.items.fundAll'),
//                     }),
//                 );
//             }
//         }
//     };
//
//     handleTabSelect = item => {
//         this.setState({ selectedTabItem: Number(item) });
//     };
//
//     renderTabContent = () => {
//         const { fund } = this.props;
//         const { selectedTabItem } = this.state;
//
//         console.log(':::selected items', this.selectedUser, this.selectedGroup);
//         console.log(':::this.state', this.state);
//
//         switch (selectedTabItem) {
//             case FundDetail.TAB_USERS:
//                 return (
//                     <FundUsersPanel
//                         fundId={fund.id}
//                         onSelectItem={(item, index) => {
//                             this.selectedUser = { index, id: item.id };
//                         }}
//                         selectedPermission={this.selectedUser}
//                     />
//                 );
//             case FundDetail.TAB_GROUPS:
//                 return (
//                     <FundGroupsPanel
//                         fundId={fund?.id}
//                         onSelectItem={(item, index) => {
//                             this.selectedGroup = { index, id: item.id };
//                         }}
//                         selectedPermission={this.selectedGroup}
//                     />
//                 );
//             default:
//                 return null;
//         }
//     };
//
//     render() {
//         const { fund } = this.props;
//         const { selectedTabItem } = this.state;
//
//         if (!fund.fetched || fund.isFetching) {
//             return <HorizontalLoader />;
//         }
//
//         return (
//             <AdminRightsContainer
//                 className="detail-container"
//                 header={
//                     <DetailHeader
//                         icon={<Icon glyph="fa-group" />}
//                         title={fund.data.name}
//                         flagLeft={i18n('admin.fund.title')}
//                         subtitle={fund.data.internalCode}
//                     />
//                 }
//             >
//                 <div className="permissions-container">
//                     <Tabs.Container>
//                         <Tabs.Tabs
//                             asTabs
//                             items={FundDetail.tabItems}
//                             activeItem={selectedTabItem}
//                             onSelect={this.handleTabSelect}
//                         />
//                         <Tabs.Content>{this.renderTabContent()}</Tabs.Content>
//                     </Tabs.Container>
//                 </div>
//             </AdminRightsContainer>
//         );
//     }
// }
//
// function mapStateToProps(state) {
//     return {
//         fund: storeFromArea(state, fundActions.AREA_ADMIN_FUND),
//     };
// }
//
// export default connect(mapStateToProps)(FundDetail);
export default FundDetailFn;
