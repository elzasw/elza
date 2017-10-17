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

class FundDetail extends AbstractReactComponent {
    static TAB_USERS = 0;
    static TAB_GROUPS = 1;

    static tabItems = [
        {id: FundDetail.TAB_USERS, title: i18n("admin.perms.fund.tabs.users")},
        {id: FundDetail.TAB_GROUPS, title: i18n("admin.perms.fund.tabs.groups")},
    ];

    constructor(props) {
        super(props);

        this.state = {
            selectedTabItem: FundDetail.tabItems[FundDetail.TAB_USERS]
        }
    }

    componentDidMount() {
        const {fund} = this.props;
        this.props.dispatch(fundActions.fundFetchIfNeeded(fund.id));
    }

    componentWillReceiveProps(nextProps) {
        const {fund} = nextProps;
        this.props.dispatch(fundActions.fundFetchIfNeeded(fund.id));
    }

    handleTabSelect = (item) => {
        this.setState({selectedTabItem: item});
    };

    renderTabContent = () => {
        const {fund} = this.props;
        const {selectedTabItem} = this.state;

        switch (selectedTabItem.id) {
            case FundDetail.TAB_USERS:
                return <FundUsersPanel fundId={fund.id} />
            case FundDetail.TAB_GROUPS:
                return <FundGroupsPanel fundId={fund.id} />
        }
    };

    render() {
        const {fund} = this.props;
        const {selectedTabItem} = this.state;

        if (!fund.fetched || fund.isFetching) {
            return <HorizontalLoader/>
        }

        return <AdminRightsContainer
            header={<div>
                <h1>{fund.data.name}</h1>
            </div>}
        >
            <Tabs.Container>
                <Tabs.Tabs items={FundDetail.tabItems}
                           activeItem={selectedTabItem}
                           onSelect={this.handleTabSelect}
                />
                <Tabs.Content>
                    {this.renderTabContent()}
                </Tabs.Content>
            </Tabs.Container>
        </AdminRightsContainer>
    }
};

function mapStateToProps(state) {
    return {
        fund: storeFromArea(state, fundActions.AREA_ADMIN_FUND),
    }}

export default connect(mapStateToProps)(FundDetail);
