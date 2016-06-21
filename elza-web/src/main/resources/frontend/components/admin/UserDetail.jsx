import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button, Input} from 'react-bootstrap'
import {Icon, AbstractReactComponent, NoFocusButton, AddRemoveList, i18n, Loading} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {usersUserDetailFetchIfNeeded} from 'actions/admin/user.jsx'
import * as perms from 'actions/user/Permission.jsx';

require ('./UserDetail.less');

var UserDetail = class UserDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods("renderPermission");
    }

    componentDidMount() {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    renderPermission(permission) {

        const permInfo = perms.all[permission.permission]

        const permInput = (
            <Input type="select" value={permission.permission}>
                <option />
                {Object.keys(perms.all).map(perm => {
                    return <option value={perm}>{i18n("permission." + perm)}</option>
                })}
            </Input>
        )

        var permValue;
        if (permInfo && (permInfo.fund || permInfo.scope)) {
            if (permInfo.fund) {
                permValue = (
                    <Input type="text" value={permission.fundId} />
                )
            } else if (permInfo.scope) {
                permValue = (
                    <Input type="text" value={permission.scopeId} />
                )
            }
        } else {
            permValue = <div className="form-group"></div>
        }

        return (
            <div className="permission-container">
                {permInput}
                {permValue}
            </div>
        )
    }

    render() {
        const {userDetail, focus} = this.props;

        if (userDetail.id === null) {
            return <div className='user-detail-container'></div>
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
                    onAdd={this.handleAddGroup}
                    onRemove={this.handleRemoveGroup}
                    addTitle="admin.user.group.action.add"
                    removeTitle="admin.user.group.action.delete"
                    />
                <h2>{i18n("admin.user.title.permissions")}</h2>
                <AddRemoveList
                    items={userDetail.permissions}
                    onAdd={this.handleAddPermission}
                    onRemove={this.handleRemovePermission}
                    addTitle="admin.user.permission.action.add"
                    removeTitle="admin.user.permission.action.delete"
                    renderItem={this.renderPermission}
                    />
            </div>
        );
    }
};

UserDetail.propTypes = {
    userDetail: React.PropTypes.object.isRequired
};

function mapStateToProps(state) {
    return {
    }
}

module.exports = connect(mapStateToProps)(UserDetail);

