/**
 *  Souborová ListBox komponenta
 *
 **/

require('./FileListBox.less');

import React from "react";
import i18n from "../../i18n";
import ListBox from "./ListBox";
import Search from "../search/Search";
import Icon from "../icon/Icon";
import AbstractReactComponent from "../../AbstractReactComponent";


var __FileListBox_timer = null;

const FileListBox = class FileListBox extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'renderItemContent',
            'handleSearch',
            'handleSearchClear',
            'handleSearchChange',
            'focus'
        );

        this.state = {
            filterText: props.filterText
        };
    }

    componentWillReceiveProps(nextProps) {}

    renderItemContent(item) {
        const {onDelete, onDownloadPdf, supportDownloadPdf, onReplace, onEdit, supportEdit, onDownload, onInfo} = this.props;
        let iconName;
        switch (item.mimeType) {
            case 'application/pdf':
                iconName = 'fa-file-pdf-o';
                break;
            case 'application/zip':
                iconName = 'fa-file-archive-o';
                break;
            default:
                iconName = 'fa-file-o';
        }

        const showEdit = onEdit && (!supportEdit || supportEdit(item.id, item))
        const showDownloadPdf = onDownloadPdf && (!supportDownloadPdf || supportDownloadPdf(item.id, item))

        return (
            <div key={'file-id-' + item.id} className="search-result-row">
                <div className="details">
                    <div className="header">
                        <Icon glyph={iconName} />
                        <div title={item.name} className="title">{item.name}</div>
                    </div>
                    <div className="path" >{item.fileName}</div>
                </div>
                <div className="actions">
                    {onInfo && <Icon glyph='fa-info-circle' onClick={() => onInfo(item.id)} />}
                    {showEdit && <Icon title={i18n("global.action.update")} glyph='fa-edit' onClick={() => onEdit(item.id)} />}
                    {showDownloadPdf && <Icon title={i18n("global.action.download")} glyph='fa-file-pdf-o' onClick={() => onDownloadPdf(item.id)} />}
                    {onDownload && <Icon title={i18n("global.action.download")} glyph='fa-download' onClick={() => onDownload(item.id)} />}
                    {onReplace && <Icon title={i18n("global.action.replace")} glyph='fa-exchange' onClick={() => onReplace(item.id)} />}
                    {onDelete && <Icon title={i18n("global.action.delete")} glyph='fa-trash' onClick={() => onDelete(item.id)} />}
                </div>
            </div>
        )
    }

    handleSearchChange(e) {
        this.setState({
            filterText: e.target.value,
        }, ()=> {
            if (__FileListBox_timer) {
                clearTimeout(__FileListBox_timer)
            }
            __FileListBox_timer = setTimeout(this.handleSearch, 250);
        })
    }

    handleSearch() {
        const {onSearch} = this.props;
        onSearch && onSearch(this.state.filterText)
    }

    handleSearchClear() {
        this.setState({
            filterText: '',
        }, () => this.handleSearch())
    }

    focus() {
        this.refs.listBox.focus()
    }

    render() {
        const {className, items, searchable, renderItemContent} = this.props;
        const {filterText} = this.state;

        var cls = "file-listbox-container";
        if (className) {
            cls += " " + className;
        }

        return (
            <div className={cls}>
                {searchable && <div className='search-container'>
                    <Search
                        placeholder={i18n('search.input.search')}
                        filterText={filterText}
                        onChange={this.handleSearchChange}
                        onSearch={this.handleSearch}
                        onClear={this.handleSearchClear}
                    />
                </div>}
                <div className='list-container'>
                    <ListBox
                        ref="listBox"
                        items={items}
                        renderItemContent={renderItemContent ? renderItemContent : this.renderItemContent}
                        onCheck={this.handleCheckItem}
                    />
                </div>
            </div>
        )
    }
};

FileListBox.propsTypes = {
    onDownload: React.PropTypes.func,
    onReplace: React.PropTypes.func,
    onEdit: React.PropTypes.func,
    onInfo: React.PropTypes.func,
    onDelete: React.PropTypes.func,
    onDownloadPdf: React.PropTypes.func,

    supportEdit: React.PropTypes.func,
    supportDownloadPdf: React.PropTypes.func,

    className:React.PropTypes.string,
    items: React.PropTypes.array.isRequired,
    renderItemContent: React.PropTypes.func,
    searchable: function(props, propName, componentName) {
        if (props.searchable === true) {
            if (props.onSearch && !(typeof props.onSearch !== Function)) {
                return new Error('Invalid prop `onSearch` supplied to `' + componentName + '`. On searchable is allowed Function only in onSearch.');
            }
        }
    },
    onSearch: React.PropTypes.func,
    onChange: React.PropTypes.func,
    selectionType: React.PropTypes.string
};

FileListBox.defaultProps = {
    searchable: false,
    filterText: ''
};

export default FileListBox;
