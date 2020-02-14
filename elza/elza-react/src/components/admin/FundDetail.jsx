// --
import React from 'react';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, Tabs, i18n} from 'components/shared';
import AdminRightsContainer from "./AdminRightsContainer";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as fundActions from "../../actions/admin/fund";
import AddRemoveList from "../shared/list/AddRemoveList";
import {HorizontalLoader} from "../shared/index";
import FundUsersPanel from "./FundUsersPanel";
import FundGroupsPanel from "./FundGroupsPanel";
import FundsPermissionPanel from "./FundsPermissionPanel";
import DetailHeader from "../shared/detail/DetailHeader";
import "./FundDetail.scss"

class FundDetail extends AbstractReactComponent {
    static TAB_USERS = 0;
    static TAB_GROUPS = 1;

    static tabItems = [
        {id: FundDetail.TAB_USERS, title: i18n("admin.perms.fund.tabs.users")},
        {id: FundDetail.TAB_GROUPS, title: i18n("admin.perms.fund.tabs.groups")},
    ];

    /*
     * Template for selected items
     */
    defaultSelectedItem = {
        id: null,
        index: 0
    }
    /*
     * Last selected fund item.
     */
    selectedUser = this.defaultSelectedItem;
    /*
     * Last selected scope item.
     */
    selectedGroup = this.defaultSelectedItem;

    constructor(props) {
        super(props);

        this.state = {
            selectedTabItem: FundDetail.tabItems[FundDetail.TAB_USERS]
        }
    }

    componentDidMount() {
        this.fetchData(this.props);
    }

    componentWillReceiveProps(nextProps) {
        const fundId = this.props.fund.id;
        const nextFundId = nextProps.fund.id;

        // Reset selected permissions
        if(fundId !== nextFundId){
            this.selectedUser = this.defaultSelectedItem;
            this.selectedGroup = this.defaultSelectedItem;
        }

        this.fetchData(nextProps);
    }

    fetchData = (props) => {
        const {fund} = props;
        if (fund.id !== FundsPermissionPanel.ALL_ID) {
            props.dispatch(fundActions.fundFetchIfNeeded(fund.id));
        } else {
            if (!fund.data || fund.data.id !== FundsPermissionPanel.ALL_ID) {
                props.dispatch(fundActions.setFund({id: FundsPermissionPanel.ALL_ID, name: i18n("admin.perms.tabs.funds.items.fundAll")}))
            }
        }
    };

    handleTabSelect = (item) => {
        this.setState({selectedTabItem: item});
    };

    renderTabContent = () => {
        const {fund} = this.props;
        const {selectedTabItem} = this.state;
        console.log("selected items", this.selectedUser, this.selectedGroup);

        switch (selectedTabItem.id) {
            case FundDetail.TAB_USERS:
                return <FundUsersPanel
                    fundId={fund.id}
                    onSelectItem={(item, index) => {this.selectedUser = {index, id: item.id}}}
                    selectedPermission={this.selectedUser}
                />
            case FundDetail.TAB_GROUPS:
                return <FundGroupsPanel
                    fundId={fund.id}
                    onSelectItem={(item, index) => {this.selectedGroup = {index, id: item.id}}}
                    selectedPermission={this.selectedGroup}
                />
        }
    };

    render() {
        const {fund} = this.props;
        const {selectedTabItem} = this.state;

        if (!fund.fetched || fund.isFetching) {
            return <HorizontalLoader/>
        }

        return <AdminRightsContainer
                className="detail-container"
                header={<DetailHeader
                icon={<Icon glyph="fa-group"/>}
                title={fund.data.name}
                flagLeft={i18n("admin.fund.title")}
                subtitle={fund.data.internalCode}
            />}
        >
            <div className="permissions-container">
                <Tabs.Container>
                    <Tabs.Tabs items={FundDetail.tabItems}
                        activeItem={selectedTabItem}
                        onSelect={this.handleTabSelect}
                    />
                    <Tabs.Content>
                        {this.renderTabContent()}
                    </Tabs.Content>
                </Tabs.Container>
            </div>
        </AdminRightsContainer>
    }
};

function mapStateToProps(state) {
    return {
        fund: storeFromArea(state, fundActions.AREA_ADMIN_FUND),
    }}

export default connect(mapStateToProps)(FundDetail);
