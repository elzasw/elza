/**
 * Komponenta zobrazení požadavků ve frontě.
 */
import React from 'react';
import {connect} from 'react-redux';
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {getIndexStateFetchIfNeeded, reindex} from 'actions/admin/fulltext.jsx';
import {Ribbon, AdminPackagesList, AdminPackagesUpload} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import * as digitizationActions from 'actions/arr/digitizationActions';

const AdminRequestsQueuePage = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }


    componentDidMount() {
        // this.dispatch(digitizationActions.fetchInQueueListIfNeeded(fund.versionId));
        // fetchInQueueListIfNeeded
    }

    componentWillReceiveProps(nextProps) {
        // this.dispatch(digitizationActions.fetchInQueueListIfNeeded(fund.versionId));
    }

    render() {
        const {splitter} = this.props;

        var centerPanel = (
            <div>
            </div>
        )

        return (
            <PageLayout
                splitter={splitter}
                className='admin-requestsQueue-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
};

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

export default connect(mapStateToProps)(AdminRequestsQueuePage);
