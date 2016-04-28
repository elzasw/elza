/**
 * Stránka pro správu importovaných balíčků
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import ReactDOM from 'react-dom';

require ('./AdminPackagesPage.less');

import {connect} from 'react-redux'
import {Ribbon, AdminPackagesList, AdminPackagesUpload} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import {UrlFactory} from 'actions/index.jsx';

var AdminPackagesPage = class AdminPackagesPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }

    render() {
        const {splitter} = this.props;

        var centerPanel = (
            <div>
                <AdminPackagesList getExportUrl={UrlFactory.exportPackage} {...this.props.packages} />
                <AdminPackagesUpload />
            </div>
        )

        return (
            <PageLayout
                splitter={splitter}
                className='admin-packages-page'
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
    const {splitter, adminRegion: {packages}} = state
    return {
        splitter,
        packages
    }
}

module.exports = connect(mapStateToProps)(AdminPackagesPage);
