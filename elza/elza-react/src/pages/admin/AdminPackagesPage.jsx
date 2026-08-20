import React from 'react';

import { connect } from 'react-redux';
import { AdminPackagesUpload, Ribbon } from 'components';
import { UrlFactory } from 'actions/index.jsx';

import './AdminPackagesPage.scss';
import { AdminLayout } from '../shared/layout/AdminLayout';
import { AdminPackagesListFn } from 'components/admin/AdminPackagesList';

/**
 * Stránka pro správu importovaných balíčků
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
class AdminPackagesPage extends React.Component {
    buildRibbon = () => {
        return <Ribbon {...this.props} />;
    };

    render() {

        const centerPanel = (
            <div>
                <AdminPackagesListFn getExportUrl={UrlFactory.exportPackage} />
                <AdminPackagesUpload />
            </div>
        );

        return (
            <AdminLayout
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
        adminRegion: { packages },
    } = state;
    return {
        packages,
    };
}

export default connect(mapStateToProps)(AdminPackagesPage);
