import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, StoreHorizontalLoader} from 'components/shared';
import {Button} from 'react-bootstrap'
import {fetchFundOutputFilesIfNeeded, fundOutputFilesFilterByText} from 'actions/arr/fundOutputFiles.jsx'
import {UrlFactory} from 'actions/index.jsx';

import './FundFiles.scss';
import './FundOutputFiles.scss';
import {downloadFile} from "../../actions/global/download";

/**
 * Správa souborů.
 */
class FundOutputFiles extends AbstractReactComponent {

    static propTypes = {
        outputResultId: PropTypes.number.isRequired,
        versionId: PropTypes.number.isRequired,
        fundOutputFiles: PropTypes.object.isRequired
    };

    state = {
        selectedId: 0
    };

    componentDidMount() {
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {versionId, outputResultId} = props;
        this.props.dispatch(fetchFundOutputFilesIfNeeded(versionId, outputResultId));
    };

    handleTextSearch = (text) => {
        const {versionId} = this.props;
        this.props.dispatch(fundOutputFilesFilterByText(versionId, text));
    };

    handleDownload = (id) => {
        this.props.dispatch(downloadFile(UrlFactory.downloadDmsFile(id)));
    };

    handleDownloadAll = () => {
        const {versionId, outputResultId} = this.props;
        this.props.dispatch(downloadFile(UrlFactory.downloadOutputResult(outputResultId)));
    };

    focus = () => {
        this.refs.listBox.focus()
    };

    render() {
        const {fundOutputFiles} = this.props;

        return <div className='fund-files fund-output-files'>
            <div className={"fund-files-header"}>
                <h4 className={"fund-files-title"}>{i18n("arr.output.title.complete")}</h4>
                <Button bsStyle="action" className={"fund-files-download-all"} onClick={this.handleDownloadAll}>
                    <Icon className={"fund-files-download-icon"} title={i18n("global.action.download")} glyph='fa-download'/>
                    {i18n('global.action.downloadAll')}
                </Button>
            </div>

            <StoreHorizontalLoader store={fundOutputFiles}/>

            {fundOutputFiles.fetched && <FileListBox
                ref="listBox"
                items={fundOutputFiles.data.rows}
                filterText={fundOutputFiles.filterText}
                onSearch={this.handleTextSearch}
                onDownload={this.handleDownload}
            />}
        </div>
    }
}


export default connect()(FundOutputFiles);
