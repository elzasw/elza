import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import {Ribbon, AdminBundleList, i18n, Icon, RibbonGroup} from 'components';
import {UrlFactory} from 'actions/index.jsx';
import {Button} from 'react-bootstrap'

import './AdminPackagesPage.less';
import PageLayout from "../shared/layout/PageLayout";


class AdminBundleActionPage extends React.Component {

    buildRibbon = () => {
        return (
            <Ribbon admin {...this.props} />
        )
    };

    render() {
        const {splitter} = this.props;

        const centerPanel = <div>
            <AdminBundleList />
        </div>;

        return (
            <PageLayout
                splitter={splitter}
                className='admin-bundleAction-page'
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

export default connect(mapStateToProps)(AdminBundleActionPage);
