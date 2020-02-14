import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, StoreHorizontalLoader, FormInput} from 'components/shared';
import AddFileForm from './AddFileForm'
import {Button, DropdownButton, MenuItem} from 'react-bootstrap'
import {fetchFundFilesIfNeeded, fundFilesFilterByText, fundFilesCreate, fundFilesEditEditable, fundFilesDelete, fundFilesReplace, fundFilesUpdate} from 'actions/arr/fundFiles.jsx'
import {modalDialogShow,modalDialogHide} from 'actions/global/modalDialog.jsx'
import {UrlFactory} from 'actions/index.jsx';

import './FundFiles.scss'
import {downloadFile} from "../../actions/global/download";
import EditableFileForm from "./EditableFileForm";
import {WebApi} from "../../actions/WebApi";
import {showAsyncWaiting} from "../../actions/global/modalDialog";
import TooltipTrigger from "../shared/tooltip/TooltipTrigger";

import * as dms from "../../actions/global/dms";
import storeFromArea from "../../shared/utils/storeFromArea";

let _ReplaceId = null;

/**
 * Správa souborů.
 */
class FundFiles extends AbstractReactComponent {

    static propTypes = {
        fundId: PropTypes.number.isRequired,
        versionId: PropTypes.number.isRequired,
        files: PropTypes.array,
        filterText: PropTypes.string.isRequired,
        fetched: PropTypes.bool.isRequired,
        fundFiles: PropTypes.object.isRequired,   // store fund files
        dms: PropTypes.object.isRequired,
        readMode: PropTypes.bool,
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
        this.props.dispatch(fetchFundFilesIfNeeded(versionId, fundId));
        this.props.dispatch(dms.mimeTypesFetchIfNeeded());
    };

    handleTextSearch = (text) => {
        const {versionId} = this.props;
        this.props.dispatch(fundFilesFilterByText(versionId, text));
    };

    handleEdit = (id) => {
        const {fundId} = this.props;
        this.props.dispatch(showAsyncWaiting(null, null, WebApi.getEditableFundFile(fundId, id), dmsFile => {
            const form = <EditableFileForm initialValues={dmsFile} onSubmitForm={data => this.handleEditEditableSubmit(id, data)} />;
            this.props.dispatch(modalDialogShow(this, i18n('dms.file.title.editable.edit'), form));
        }));
    };

    handleDownloadByMimeType = (id, outputMimeType) => {
        const {fundId} = this.props;
        // window.open(`/api/dms/fund/${fundId}/${id}/generated?mimeType=${outputMimeType}`);
        this.props.dispatch(downloadFile(UrlFactory.downloadGeneratedDmsFile(id, fundId, outputMimeType)));
    };

    handleDelete = (id) => {
        const {fundId, versionId} = this.props;
        this.props.dispatch(fundFilesDelete(versionId, fundId, id));
    };

    handleCreateFromFile = () => {
        this.props.dispatch(modalDialogShow(this, i18n('dms.file.title.file.add'), <AddFileForm onSubmitForm={this.handleCreateFromFileSubmit} />));
    };

    handleCreateEditable = () => {
        if(this.hasMimeTypes()){
            this.props.dispatch(modalDialogShow(this, i18n('dms.file.title.editable.add'), <EditableFileForm create onSubmitForm={this.handleCreateEditableSubmit} />));
        }
    };

    handleCreateFromFileSubmit = (data) => {
        const {fundId} = this.props;
        return this.props.dispatch(fundFilesCreate(fundId, data));
    };

    handleCreateEditableSubmit = (data) => {
        const {fundId} = this.props;
        return this.props.dispatch(fundFilesCreate(fundId, data));
    };

    handleEditEditableSubmit = (id, data) => {
        const {fundId} = this.props;
        return this.props.dispatch(fundFilesUpdate(fundId, id, data, () => {
            this.props.dispatch(modalDialogHide());
        }));
    };

    handleDownload = (id) => {
        this.props.dispatch(downloadFile(UrlFactory.downloadDmsFile(id)));
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

        this.props.dispatch(fundFilesReplace(_ReplaceId, file));
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

    hasMimeTypes = () => {
        const {dms} = this.props;
        return dms.fetched && dms.rows.length > 0;
    }

    render() {
        const {fundFiles, readMode, dms} = this.props;

        return <div className='fund-files'>
            <StoreHorizontalLoader store={fundFiles}/>

            {!readMode && fundFiles.fetched && <div className="actions-container">
                <div className="actions">
                    <DropdownButton bsStyle="default" id='dropdown-add-file' noCaret title={<Icon glyph='fa-plus-circle' />}>
                        <MenuItem onClick={this.handleCreateFromFile}>
                            {i18n("arr.fund.files.action.add.fromFile")}
                        </MenuItem>
                        <MenuItem disabled={!this.hasMimeTypes()} onClick={this.handleCreateEditable}>
                            {!this.hasMimeTypes() ?
                             <TooltipTrigger
                                 key="info"
                                 content={<div style={{maxWidth: "300px"}}>{ i18n("arr.fund.files.noMimetypeConfig") }</div>}
                                 placement="left"
                                 showDelay={1}
                                 holdOnHover
                             >
                                 {i18n("arr.fund.files.action.add.editable")}
                             </TooltipTrigger> :
                             i18n("arr.fund.files.action.add.editable")
                            }
                        </MenuItem>
                    </DropdownButton>
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
                onEdit={this.handleEdit}
                supportEdit={(id, item) => item.editable}
                onDownloadPdf={(id) => this.handleDownloadByMimeType(id, "application/pdf")}
                readMode={ readMode }
                supportDownloadPdf={(id, item) => item.generatePdf}
            />}
        </div>
    }
}

function mapStateToProps(state) {
    return {
        dms: storeFromArea(state, dms.MIME_TYPES_AREA)
    };
}

export default connect(mapStateToProps)(FundFiles);
