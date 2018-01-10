import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, StoreHorizontalLoader} from 'components/shared';
import {Button} from 'react-bootstrap'
import {fetchFundOutputFilesIfNeeded, fundOutputFilesFilterByText} from 'actions/arr/fundOutputFiles.jsx'
import {UrlFactory} from 'actions/index.jsx';

import './FundFiles.less';
import {downloadFile} from "../../actions/global/download";

/**
 * Správa souborů.
 */
class FundOutputFiles extends AbstractReactComponent {

    static PropTypes = {
        outputResultId: React.PropTypes.number.isRequired,
        versionId: React.PropTypes.number.isRequired,
        fundOutputFiles: React.PropTypes.object.isRequired
    };

    state = {
        selectedId:0
    };

    componentDidMount() {
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {versionId, outputResultId} = props;
        this.dispatch(fetchFundOutputFilesIfNeeded(versionId, outputResultId));
    };

    handleTextSearch = (text) => {
        const {versionId} = this.props;
        this.dispatch(fundOutputFilesFilterByText(versionId, text));
    };

    handleDownload = (id) => {
        this.dispatch(downloadFile(UrlFactory.downloadDmsFile(id)));
    };

    handleDownloadAll = () => {
        const {versionId, outputResultId} = this.props;
        this.dispatch(downloadFile(UrlFactory.downloadOutputResult(outputResultId)));
    };

    focus = () => {
        this.refs.listBox.focus()
    };

    render() {
        const {fundOutputFiles} = this.props;

        return <div className='fund-packets'>
            <Button onClick={this.handleDownloadAll}>{i18n('global.action.downloadAll')}</Button>

            <StoreHorizontalLoader store={fundOutputFiles} />

            {fundOutputFiles.fetched && <FileListBox
                ref="listBox"
                items={fundOutputFiles.data.rows}
                searchable
                filterText={fundOutputFiles.filterText}
                onSearch={this.handleTextSearch}
                onDownload={this.handleDownload}
            />}
        </div>
    }
}


export default connect(null, null, null, { withRef: true })(FundOutputFiles);
