import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n, StoreHorizontalLoader, AddRemoveList} from 'components/shared';
import {indexById, getIdsList} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'
import Permissions from "./Permissions.jsx"
import {changeGroupPermission} from 'actions/admin/permission.jsx'
import {joinUsers, leaveUser} from 'actions/admin/group.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {renderUserItem} from "components/admin/adminRenderUtils.jsx"
import SelectItemsForm from "./SelectItemsForm";
import UserField from "./UserField";

require('./GroupDetail.less');

const GroupDetail = class GroupDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            "handleSavePermissions",
            "handleAddUsers",
            "handleRemoveUser",
        );
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    handleSavePermissions(data) {
        const {groupDetail} = this.props;
        this.dispatch(changeGroupPermission(groupDetail.id, data));
    }

    handleRemoveUser(user, index) {
        const {groupDetail} = this.props;
        this.dispatch(leaveUser(groupDetail.id, user.id));
    }

    handleAddUsers() {
        const {groupDetail} = this.props;
        this.dispatch(modalDialogShow(this, i18n('admin.group.user.add.title'),
            <SelectItemsForm
                onSubmitForm={(users) => {
                    this.dispatch(joinUsers(groupDetail.id, getIdsList(users)));
                }}
                fieldComponent={UserField}
                fieldComponentProps={{excludedGroupId: groupDetail.id}}
                renderItem={renderUserItem}
            />
        ));
    }

    render() {
        const {groupDetail, focus, groupCount} = this.props;

        if (groupDetail.id === null) {
            return <div className='group-detail-container'>
                        <div className="unselected-msg">
                            <div className="title">{groupCount > 0 ? i18n('admin.group.noSelection.title') : i18n('admin.group.emptyList.title')}</div>
                            <div className="msg-text">{groupCount > 0 ? i18n('admin.group.noSelection.message') : i18n('admin.group.emptyList.message')}</div>
                        </div>
                    </div>
        }

        let content;
        if (groupDetail.fetched) {
            content = <div className='group-detail-container'>
                <h1>{groupDetail.name}</h1>
                <h2>{i18n("admin.group.title.users")}</h2>
                <AddRemoveList
                    items={groupDetail.users}
                    onAdd={this.handleAddUsers}
                    onRemove={this.handleRemoveUser}
                    addTitle="admin.group.user.action.add"
                    removeTitle="admin.group.user.action.delete"
                    renderItem={renderUserItem}
                />
                <h2>{i18n("admin.group.title.permissions")}</h2>
                <Permissions
                    area="GROUP"
                    initData={{permissions: groupDetail.permissions}}
                    onSave={this.handleSavePermissions}
                    addTitle="admin.group.permission.action.add"
                    removeTitle="admin.group.permission.action.delete"
                />
            </div>;
        }

        return <div>
            <StoreHorizontalLoader store={groupDetail}/>
            {content}
        </div>;
    }
};

GroupDetail.propTypes = {
    groupDetail: React.PropTypes.object.isRequired,
    groupCount: React.PropTypes.number.isRequired
};

function mapStateToProps(state) {
    return {
    }
}

export default connect(mapStateToProps)(GroupDetail);
