
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
import Permissions from "./Permissions.jsx"
import * as perms from 'actions/user/Permission.jsx';

require ('./UserDetail.less');

const UserDetail = class UserDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        // this.bindMethods("");
    }

    componentDidMount() {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(usersUserDetailFetchIfNeeded())
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
                <Permissions
                    area="USER"
                    permissions={userDetail.permission.permissions}
                    addTitle="admin.user.permission.action.add"
                    removeTitle="admin.user.permission.action.delete"
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

