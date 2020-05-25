import React from 'react';

import {connect} from 'react-redux'
import {AdminBulkList, Ribbon} from 'components';

import './AdminPackagesPage.scss';
import PageLayout from "../shared/layout/PageLayout";


class AdminBulkActionPage extends React.Component {

    buildRibbon = () => {
        return (
            <Ribbon admin {...this.props} />
        )
    };

    render() {
        const {splitter} = this.props;

        const centerPanel = <div>
            <AdminBulkList />
        </div>;

        return (
            <PageLayout
                splitter={splitter}
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
    const {splitter, adminRegion: {packages}} = state;
    return {
        splitter,
        packages
    }
}

export default connect(mapStateToProps)(AdminBulkActionPage);
