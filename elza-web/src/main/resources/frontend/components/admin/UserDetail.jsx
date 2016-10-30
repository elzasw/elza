
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button, Input} from 'react-bootstrap'
import {Icon, AbstractReactComponent, NoFocusButton, AddRemoveList, i18n, Loading} from 'components/index.jsx';
import {indexById, getIdsList} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {changeUserPermission} from 'actions/admin/permission.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {usersUserDetailFetchIfNeeded} from 'actions/admin/user.jsx'
import Permissions from "./Permissions.jsx"
import {joinGroups, leaveGroup} from 'actions/admin/user.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {renderGroupItem} from "components/admin/adminRenderUtils.jsx"
import SelectGroupsForm from './SelectGroupsForm.jsx'
import * as perms from 'actions/user/Permission.jsx';

require ('./UserDetail.less');

const UserDetail = class UserDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            "handleSavePermissions",
            "handleRemoveGroup",
            "handleAddGroups",
        );
    }

    componentDidMount() {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    handleSavePermissions(data) {
        const {userDetail} = this.props;
        this.dispatch(changeUserPermission(userDetail.id, data));
    }

    handleRemoveGroup(group, index) {
        const {userDetail} = this.props;
        this.dispatch(leaveGroup(userDetail.id, group.id));
    }

    handleAddGroups() {
        const {userDetail} = this.props;
        this.dispatch(modalDialogShow(this, i18n('admin.user.group.add.title'),
            <SelectGroupsForm onSubmitForm={(groups) => {
                this.dispatch(joinGroups(userDetail.id, getIdsList(groups)));
            }} />
        ))
    }

    render() {
        const {userDetail, focus, userCount} = this.props;

        if (userDetail.id === null) {
            return(
                <div className='user-detail-container'>
                    <div className="unselected-msg">
                        <div className="title">{userCount > 0 ? i18n('admin.user.noSelection.title') : i18n('admin.user.emptyList.title')}</div>
                        <div className="message">{userCount > 0 ? i18n('admin.user.noSelection.message') : i18n('admin.user.emptyList.message')}</div>
                    </div>
                </div>);
        }

        if (!userDetail.fetched) {
            return <div className='user-detail-container'><Loading/></div>
        }

        return (
            <div className='user-detail-container'>
                <h1>{userDetail.party.record.record}</h1>
                <div>{i18n("admin.user.label.username")}</div>
                <div>{userDetail.username}</div>
                <h2>{i18n("admin.user.title.groups")}</h2>
                <AddRemoveList
                    items={userDetail.groups}
                    onAdd={this.handleAddGroups}
                    onRemove={this.handleRemoveGroup}
                    addTitle="admin.user.group.action.add"
                    removeTitle="admin.user.group.action.delete"
                    renderItem={renderGroupItem}
                    />
                <h2>{i18n("admin.user.title.permissions")}</h2>
                <Permissions
                    area="USER"
                    initData={{permissions: userDetail.permissions}}
                    onSave={this.handleSavePermissions}
                    addTitle="admin.user.permission.action.add"
                    removeTitle="admin.user.permission.action.delete"
                    />
            </div>
        );
    }
};

UserDetail.propTypes = {
    userDetail: React.PropTypes.object.isRequired,
    userCount: React.PropTypes.object.isRequired,
};

function mapStateToProps(state) {
    return {
    }
}

module.exports = connect(mapStateToProps)(UserDetail);

