// --
import React from 'react';
import {connect} from 'react-redux'
import {HorizontalLoader, AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import * as adminPermissions from "../../actions/admin/adminPermissions";
import storeFromArea from "../../shared/utils/storeFromArea";
import {modalDialogShow, modalDialogHide} from "../../actions/global/modalDialog";
import AdminRightsContainer from "./AdminRightsContainer";
import AddRemoveListBox from "../shared/listbox/AddRemoveListBox";
import {renderUserItem} from "./adminRenderUtils";
import FundsPermissionPanel from "./FundsPermissionPanel";
import {WebApi} from "../../actions/WebApi";
import SelectItemsForm from "./SelectItemsForm";
import getMapFromList from "../../shared/utils/getMapFromList";
import UserField from "./UserField";
import {changeUsersForFund} from "../../actions/admin/adminPermissions";

class FundUsersPanel extends AbstractReactComponent {
   constructor(props) {
        super(props);

        this.state = {
            selectedIndex: null
        }
    }

    componentDidMount() {
        const {fundId} = this.props;
        this.props.dispatch(adminPermissions.fetchUsersByFund(fundId));
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.fundId !== nextProps.fundId) {
            this.props.dispatch(adminPermissions.fetchUsersByFund(nextProps.fundId));
        }
    }

    handleAdd = () => {
        const {fundId} = this.props;

        this.props.dispatch(modalDialogShow(this, i18n('admin.perms.fund.tabs.users.add.title'),
            <SelectItemsForm
                onSubmitForm={(users) => {
                    const usersMap = getMapFromList(this.props.users.rows);
                    const newRows = [...this.props.users.rows];

                    users.forEach(user => {
                        if (!usersMap[user.id]) { // jen pokud ještě přidaný není
                            newRows.push({
                                ...user,
                                permissions: [],
                                groups: []
                            });
                        }
                    });

                    this.props.dispatch(changeUsersForFund(fundId, newRows));
                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={UserField}
                renderItem={renderUserItem}
            />
        ));
    };

    handleRemove = (item, index) => {
        const {users, fundId} = this.props;
        WebApi.deleteUserFundPermission(item.id, fundId)
            .then(data => {
                const newRows = [
                    ...users.rows.slice(0, index),
                    ...users.rows.slice(index + 1)
                ];
                this.props.dispatch(changeUsersForFund(fundId, newRows));
            });
    };

    render() {
        const {fundId, users} = this.props;
        const {selectedIndex} = this.state;

        if (!users.fetched) {
            return <HorizontalLoader/>
        }

        const user = selectedIndex !== null ? users.rows[selectedIndex] : null;

        return <AdminRightsContainer
            left={<AddRemoveListBox
                items={users.rows}
                activeIndex={selectedIndex}
                renderItemContent={renderUserItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                onFocus={(item, index) => {
                    this.setState({selectedIndex: index})
                }}
            />}
            >
            {user && <FundsPermissionPanel
                fundId={fundId}
                userId={user.id}
                onAddPermission={perm => WebApi.addUserPermission(user.id, perm)}
                onDeletePermission={perm => WebApi.deleteUserPermission(user.id, perm)}
                onDeleteFundPermission={fundId => WebApi.deleteUserFundPermission(user.id, fundId)}
            />}
        </AdminRightsContainer>
    }
}

function mapStateToProps(state) {
    return {
        users: storeFromArea(state, adminPermissions.USERS_PERMISSIONS_BY_FUND),
    }
}

export default connect(mapStateToProps)(FundUsersPanel);
