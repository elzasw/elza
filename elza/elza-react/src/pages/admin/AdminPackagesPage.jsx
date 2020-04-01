import React from 'react';

import {connect} from 'react-redux';
import {AdminPackagesList, AdminPackagesUpload, Ribbon} from 'components';
import {UrlFactory} from 'actions/index.jsx';

import './AdminPackagesPage.scss';
import PageLayout from '../shared/layout/PageLayout';

/**
 * Stránka pro správu importovaných balíčků
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
class AdminPackagesPage extends React.Component {
    buildRibbon = () => {
        return <Ribbon admin {...this.props} />;
    };

    render() {
        const {splitter} = this.props;

        const centerPanel = (
            <div>
                <AdminPackagesList getExportUrl={UrlFactory.exportPackage} {...this.props.packages} />
                <AdminPackagesUpload />
            </div>
        );

        return (
            <PageLayout
                splitter={splitter}
                className="admin-packages-page"
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        );
    }
}

/**
 * Namapování state do properties.
 *
 * @param state state aplikace
 * @returns {{packages: *}}
 */
function mapStateToProps(state) {
    const {
        splitter,
        adminRegion: {packages},
    } = state;
    return {
        splitter,
        packages,
    };
}

export default connect(mapStateToProps)(AdminPackagesPage);
