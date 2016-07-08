/**
 * Správa souborů.
 */

require('./FundFiles.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, Loading} from 'components/index.jsx';
import {Button} from 'react-bootstrap'
import {fetchFundOutputFilesIfNeeded, fundOutputFilesFilterByText} from 'actions/arr/fundOutputFiles.jsx'
import {UrlFactory} from 'actions/index.jsx';

const FundOutputFiles = class FundOutputFiles extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleTextSearch',
            'handleDownload',
            'handleDownloadAll',
            'focus'
        );

        this.state = {
            selectedId:0
        }
    }

    componentDidMount() {
        const {versionId, outputResultId} = this.props;
        this.dispatch(fetchFundOutputFilesIfNeeded(versionId, outputResultId));
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, outputResultId} = this.props;
        this.dispatch(fetchFundOutputFilesIfNeeded(versionId, outputResultId));
    }
    
    handleTextSearch(text) {
        const {versionId} = this.props;
        this.dispatch(fundOutputFilesFilterByText(versionId, text));
    }

    handleDownload(id) {
        window.open(UrlFactory.downloadDmsFile(id))
    }

    handleDownloadAll() {
        window.open(UrlFactory.downloadOutputResult(this.props.outputResultId))
    }

    focus() {
        this.refs.listBox.focus()
    }

    render() {
        const {filterText, fetched, data} = this.props;

        if (!fetched) {
            return <Loading/>
        }

        return (
            <div className='fund-packets'>
                <Button onClick={this.handleDownloadAll}>{i18n('global.action.downloadAll')}</Button>
                <FileListBox
                    ref="listBox"
                    items={data.list}
                    searchable
                    filterText={filterText}
                    onSearch={this.handleTextSearch}
                    onDownload={this.handleDownload}
                />
            </div>
        )
    }
};

FundOutputFiles.propTypes = {
    outputResultId: React.PropTypes.number.isRequired,
    versionId: React.PropTypes.number.isRequired,
    files: React.PropTypes.array,
    filterText: React.PropTypes.string.isRequired,
    fetched: React.PropTypes.bool.isRequired
};

module.exports = connect(null, null, null, { withRef: true })(FundOutputFiles);
