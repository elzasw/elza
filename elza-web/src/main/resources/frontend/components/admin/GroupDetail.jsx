import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap'
import {Icon, AbstractReactComponent, i18n, Loading, AddRemoveList} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'
import Permissions from "./Permissions.jsx"

require('./GroupDetail.less');

const GroupDetail = class GroupDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        // this.bindMethods('')
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }


    render() {
        const {groupDetail, focus} = this.props;

        if (groupDetail.id === null) {
            return <div className='group-detail-container'></div>
        }

        if (!groupDetail.fetched) {
            return <div className='group-detail-container'><Loading/></div>
        }

        return (
            <div className='group-detail-container'>
                <h1>{groupDetail.name}</h1>
                <h2>{i18n("admin.group.title.users")}</h2>
                <AddRemoveList
                    items={groupDetail.users}
                    onAdd={this.handleCreateUserForm}
                    onRemove={this.handleRemoveUser}
                    addTitle="admin.group.user.action.add"
                    removeTitle="admin.group.user.action.delete"
                    renderItem={item => <div>{item.party.record.record} ({item.username})</div>}
                />
                <h2>{i18n("admin.group.title.permissions")}</h2>
                <Permissions
                    area="GROUP"
                    permissions={groupDetail.permission.permissions}
                    addTitle="admin.group.permission.action.add"
                    removeTitle="admin.group.permission.action.delete"
                />
            </div>
        );
    }
};

GroupDetail.propTypes = {
    groupDetail: React.PropTypes.object.isRequired
};

function mapStateToProps(state) {
    return {
    }
}

module.exports = connect(mapStateToProps)(GroupDetail);

