// --
import React from 'react';
import {connect} from 'react-redux'
import {HorizontalLoader, AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import * as adminPermissions from "../../actions/admin/adminPermissions";
import storeFromArea from "../../shared/utils/storeFromArea";
import {modalDialogShow, modalDialogHide} from "../../actions/global/modalDialog";
import AdminRightsContainer from "./AdminRightsContainer";
import AddRemoveListBox from "../shared/listbox/AddRemoveListBox";
import {renderGroupItem} from "./adminRenderUtils";
import FundsPermissionPanel from "./FundsPermissionPanel";
import {WebApi} from "../../actions/WebApi";
import SelectItemsForm from "./SelectItemsForm";
import getMapFromList from "../../shared/utils/getMapFromList";
import GroupField from "./GroupField";
import {changeGroupsForFund} from "../../actions/admin/adminPermissions";

class FundGroupsPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            selectedIndex: null
        }
    }

    componentDidMount() {
        const {fundId} = this.props;
        this.props.dispatch(adminPermissions.fetchGroupsByFund(fundId));
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.fundId !== nextProps.fundId) {
            this.props.dispatch(adminPermissions.fetchGroupsByFund(nextProps.fundId));
        }
    }

    handleAdd = () => {
        const {fundId} = this.props;

        this.props.dispatch(modalDialogShow(this, i18n('admin.perms.fund.tabs.groups.add.title'),
            <SelectItemsForm
                onSubmitForm={(groups) => {
                    const groupsMap = getMapFromList(this.props.groups.rows);
                    const newRows = [...this.props.groups.rows];

                    groups.forEach(group => {
                        if (!groupsMap[group.id]) { // jen pokud ještě přidaný není
                            newRows.push({
                                ...group,
                                permissions: [],
                                users: []
                            });
                        }
                    });

                    this.props.dispatch(changeGroupsForFund(fundId, newRows));
                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={GroupField}
                renderItem={renderGroupItem}
            />
        ));
    };

    handleRemove = (item, index) => {
        const {groups, fundId} = this.props;

        const api = fundId === FundsPermissionPanel.ALL_ID ? WebApi.deleteGroupFundAllPermission(item.id) : WebApi.deleteGroupFundPermission(item.id, fundId);
        api.then(data => {
                const newRows = [
                    ...groups.rows.slice(0, index),
                    ...groups.rows.slice(index + 1)
                ];
                this.props.dispatch(changeGroupsForFund(fundId, newRows));
            });
    };

    render() {
        const {fundId, groups} = this.props;
        const {selectedIndex} = this.state;

        if (!groups.fetched) {
            return <HorizontalLoader/>
        }

        const group = selectedIndex !== null ? groups.rows[selectedIndex] : null;

        return <AdminRightsContainer
                className="permissions-panel"
                left={<AddRemoveListBox
                items={groups.rows}
                activeIndex={selectedIndex}
                renderItemContent={renderGroupItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                onFocus={(item, index) => {
                    this.setState({selectedIndex: index})
                }}
            />}
        >
            {group && <FundsPermissionPanel
                fundId={fundId}
                groupId={group.id}
                onAddPermission={perm => WebApi.addGroupPermission(group.id, perm)}
                onDeletePermission={perm => WebApi.deleteGroupPermission(group.id, perm)}
                onDeleteFundPermission={fundId => WebApi.deleteGroupFundPermission(group.id, fundId)}
            />}
        </AdminRightsContainer>
    }
}

function mapStateToProps(state) {
    return {
        groups: storeFromArea(state, adminPermissions.GROUPS_PERMISSIONS_BY_FUND),
    }
}

export default connect(mapStateToProps)(FundGroupsPanel);
