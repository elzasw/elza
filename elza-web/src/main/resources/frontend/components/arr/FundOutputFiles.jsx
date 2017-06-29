import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, Loading} from 'components/shared';
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
        files: React.PropTypes.array,
        filterText: React.PropTypes.string.isRequired,
        fetched: React.PropTypes.bool.isRequired
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
        this.dispatch(downloadFile("arr-output-file-" + id, UrlFactory.downloadDmsFile(id)));
    };

    handleDownloadAll = () => {
        const {versionId, outputResultId} = this.props;
        this.dispatch(downloadFile("arr-output-files-all-" + versionId, UrlFactory.downloadOutputResult(outputResultId)));
    };

    focus = () => {
        this.refs.listBox.focus()
    };

    render() {
        const {filterText, isFetching, data} = this.props;

        if (isFetching || !data) {
            return <Loading/>
        }

        return <div className='fund-packets'>
            <Button onClick={this.handleDownloadAll}>{i18n('global.action.downloadAll')}</Button>
            <FileListBox
                ref="listBox"
                items={data.rows}
                searchable
                filterText={filterText}
                onSearch={this.handleTextSearch}
                onDownload={this.handleDownload}
            />
        </div>
    }
}


export default connect(null, null, null, { withRef: true })(FundOutputFiles);
