import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, StoreHorizontalLoader, FormInput} from 'components/shared';
import AddFileForm from './AddFileForm'
import {Button} from 'react-bootstrap'
import {fetchFundFilesIfNeeded, fundFilesFilterByText, fundFilesCreate, fundFilesDelete, fundFilesReplace} from 'actions/arr/fundFiles.jsx'
import {modalDialogShow,modalDialogHide} from 'actions/global/modalDialog.jsx'
import {UrlFactory} from 'actions/index.jsx';

import './FundFiles.less'
import {downloadFile} from "../../actions/global/download";

let _ReplaceId = null;

/**
 * Správa souborů.
 */
class FundFiles extends AbstractReactComponent {

    static PropTypes = {
        fundId: React.PropTypes.number.isRequired,
        versionId: React.PropTypes.number.isRequired,
        files: React.PropTypes.array,
        filterText: React.PropTypes.string.isRequired,
        fetched: React.PropTypes.bool.isRequired,
        fundFiles: React.PropTypes.object.isRequired,   // store fund files
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
        const {versionId, fundId} = props;
        this.dispatch(fetchFundFilesIfNeeded(versionId, fundId));
    };

    handleTextSearch = (text) => {
        const {versionId} = this.props;
        this.dispatch(fundFilesFilterByText(versionId, text));
    };

    handleDelete = (id) => {
        const {fundId, versionId} = this.props;
        this.dispatch(fundFilesDelete(versionId, fundId, id));
    };

    handleCreate = () => {
        this.dispatch(modalDialogShow(this, i18n('dms.file.title.add'), <AddFileForm onSubmitForm={this.handleCreateSubmit} />));
    };

    handleCreateSubmit = (data) => {
        const {fundId} = this.props;
        return this.dispatch(fundFilesCreate(fundId, data))
    };

    handleDownload = (id) => {
        this.dispatch(downloadFile(UrlFactory.downloadDmsFile(id)));
    };

    handleReplace = (id) => {
        ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).click();
        _ReplaceId = id;
    };

    handleReplaceSubmit = (e) => {
        const fileList = e.target.files;

        if (fileList.length != 1) {
            return;
        }
        const file = fileList[0];

        this.dispatch(fundFilesReplace(_ReplaceId, file));
        e.target.value = null;
        _ReplaceId = null;
    };

    focus = () => {
        if (this.refs.listBox) {
            this.refs.listBox.focus();
            return true;
        } else {
            return false;
        }
    };

    render() {
        const {fundFiles} = this.props;

        return <div className='fund-files'>
            <StoreHorizontalLoader store={fundFiles}/>

            {fundFiles.fetched && <div className="actions-container">
                <div className="actions">
                    <Button onClick={this.handleCreate} eventKey='add'><Icon glyph='fa-plus' /> {i18n('arr.fund.files.action.add')}</Button>
                </div>
            </div>}
            <FormInput className="hidden" type="file" ref='uploadInput' onChange={this.handleReplaceSubmit} />

            {fundFiles.fetched && <FileListBox
                ref="listBox"
                items={fundFiles.data.rows}
                searchable
                filterText={fundFiles.filterText}
                onSearch={this.handleTextSearch}
                onDownload={this.handleDownload}
                onReplace={this.handleReplace}
                onDelete={this.handleDelete}
            />}
        </div>
    }
}

export default connect(null, null, null, { withRef: true })(FundFiles);
