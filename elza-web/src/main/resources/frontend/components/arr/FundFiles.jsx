/**
 * Správa souborů.
 */

require('./FundFiles.less')

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, FileListBox, Loading, AddFileForm, FormInput} from 'components/index.jsx';
import {Button} from 'react-bootstrap'
import {fetchFundFilesIfNeeded, fundFilesFilterByText, fundFilesCreate, fundFilesDelete, fundFilesReplace} from 'actions/arr/fundFiles.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {UrlFactory} from 'actions/index.jsx';

var _ReplaceId = null;

const FundFiles = class FundFiles extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleTextSearch',
            'handleDelete',
            'handleCreate',
            'handleCreateSubmit',
            'handleDownload',
            'handleReplace',
            'handleReplaceSubmit',
            'focus'
        );

        this.state = {
            selectedId:0
        }
    }

    componentDidMount() {
        const {versionId, fundId} = this.props;
        this.dispatch(fetchFundFilesIfNeeded(versionId, fundId));
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, fundId} = this.props;
        this.dispatch(fetchFundFilesIfNeeded(versionId, fundId));
    }
    handleTextSearch(text) {
        const {versionId} = this.props;
        this.dispatch(fundFilesFilterByText(versionId, text));
    }

    handleDelete(id) {
        const {fundId, versionId} = this.props;
        this.dispatch(fundFilesDelete(versionId, fundId, id));
    }

    handleCreate() {
        this.dispatch(modalDialogShow(this, i18n('dms.file.title.add'), <AddFileForm onSubmitForm={this.handleCreateSubmit.bind(this)} />));
    }

    handleCreateSubmit(data) {
        const {fundId} = this.props;
        this.dispatch(fundFilesCreate(fundId, data))
    }

    handleDownload(id) {
        window.open(UrlFactory.downloadDmsFile(id))
    }

    handleReplace(id) {
        ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).click();
        _ReplaceId = id;
    }

    handleReplaceSubmit(e) {
        const fileList = e.target.files;
        
        if (fileList.length != 1) {
            return;
        }
        const file = fileList[0];

        this.dispatch(fundFilesReplace(_ReplaceId, file));
        e.target.value = null;
        _ReplaceId = null;
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
            <div className='fund-files'>
                <div className="actions-container">
                    <div className="actions">
                        <Button onClick={this.handleCreate} eventKey='add'><Icon glyph='fa-plus' /> {i18n('arr.fund.files.action.add')}</Button>
                    </div>
                </div>
                <FormInput className="hidden" type="file" ref='uploadInput' onChange={this.handleReplaceSubmit} />

                <FileListBox
                    ref="listBox"
                    items={data.list}
                    searchable
                    filterText={filterText}
                    onSearch={this.handleTextSearch}
                    onDownload={this.handleDownload}
                    onReplace={this.handleReplace}
                    onDelete={this.handleDelete}
                />
            </div>
        )
    }
};

FundFiles.propTypes = {
    fundId: React.PropTypes.number.isRequired,
    versionId: React.PropTypes.number.isRequired,
    files: React.PropTypes.array,
    filterText: React.PropTypes.string.isRequired,
    fetched: React.PropTypes.bool.isRequired
};

module.exports = connect(null, null, null, { withRef: true })(FundFiles);
