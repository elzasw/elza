import React from 'react';

import {connect} from 'react-redux'
import {AdminBulkList, Ribbon} from 'components';

import './AdminPackagesPage.scss';
import { AdminLayout } from "../shared/layout/AdminLayout";


class AdminBulkActionPage extends React.Component {

    buildRibbon = () => {
        return (
            <Ribbon {...this.props} />
        )
    };

    render() {

        const centerPanel = <div>
            <AdminBulkList />
        </div>;

        return (
            <AdminLayout
                className='admin-bulkAction-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{packages: *}}
 */
function mapStateToProps(state) {
    const {adminRegion: {packages}} = state;
    return {
        packages
    }
}

export default connect(mapStateToProps)(AdminBulkActionPage);
