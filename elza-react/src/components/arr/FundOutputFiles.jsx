import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, FileListBox, Icon, StoreHorizontalLoader} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { arrPanelMessages } from './panelMessages';
import {Button} from '../ui';
import {fetchFundOutputFilesIfNeeded, fundOutputFilesFilterByText} from 'actions/arr/fundOutputFiles.jsx';
import {UrlFactory} from 'actions/index.jsx';

import './FundFiles.scss';
import './FundOutputFiles.scss';
import {downloadFile} from '../../actions/global/download';
import { FileGrid } from "components/shared/file-grid";

/**
 * Správa souborů.
 */
class FundOutputFiles extends AbstractReactComponent {
    static propTypes = {
        outputId: PropTypes.number.isRequired,
        versionId: PropTypes.number.isRequired,
        fundOutputFiles: PropTypes.object.isRequired,
        outputResultIds: PropTypes.array.isRequired,
    };

    state = {
        selectedId: 0,
    };

    componentDidMount() {
        this.fetchIfNeeded();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {versionId, outputId} = props;
        this.props.dispatch(fetchFundOutputFilesIfNeeded(versionId, outputId));
    };

    handleTextSearch = text => {
        const {versionId} = this.props;
        this.props.dispatch(fundOutputFilesFilterByText(versionId, text));
    };

    handleDownload = id => {
        this.props.dispatch(downloadFile(UrlFactory.downloadDmsFile(id)));
    };


    handleDownloadAll = () => {
        const {outputId, outputResultIds} = this.props;
        if(outputResultIds.length === 1){
            this.props.dispatch(downloadFile(UrlFactory.downloadOutputResult(outputResultIds[0])));
        } else {
            this.props.dispatch(downloadFile(UrlFactory.downloadOutputResults(outputId)));
        }
    };

    focus = () => {
        this.refs.listBox.focus();
    };


    render() {
        const {fundOutputFiles} = this.props;

        return (
            <div className="fund-files fund-output-files">
                <div className={'fund-files-header'}>
                    <div className="fund-files-title">
                        {<FormattedMessage {...arrPanelMessages.outputTitleComplete} />}
                    </div>
                    <Button 
                        variant="action" 
                        className="fund-files-download-all" 
                        onClick={this.handleDownloadAll}
                    >
                        <Icon
                            className={'fund-files-download-icon'}
                            title={this.props.intl.formatMessage(globalMessages.download)}
                            glyph="fa-download"
                        />
                        {<FormattedMessage {...arrPanelMessages.globalActionDownloadAll} />}
                    </Button>
                </div>

                <StoreHorizontalLoader store={fundOutputFiles} />

                {fundOutputFiles.fetched && 
                    <FileGrid 
                        items={fundOutputFiles.data.rows}
                        onDownload={this.handleDownload}
                    />
                }
            </div>
        );
    }
}

export default connect()(injectIntl(FundOutputFiles));
