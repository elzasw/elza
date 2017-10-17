// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import * as adminPermissions from "../../actions/admin/adminPermissions";
import storeFromArea from "../../shared/utils/storeFromArea";

class FundUsersPanel extends AbstractReactComponent {
    componentDidMount() {
        const {fundId} = this.props;
        this.props.dispatch(adminPermissions.fetchUsersByFund(fundId));
    }

    render() {
        const {users} = this.props;
        console.log(111, users)

        return <div>FundUsersPanel</div>
    }
}

function mapStateToProps(state) {
    return {
        users: storeFromArea(state, adminPermissions.ENTITIES_PERMISSIONS_BY_FUND),
    }
}

export default connect(mapStateToProps)(FundUsersPanel);
