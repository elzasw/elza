/**
 *  ListBox komponenta s možností filtrování, hledání a označování.
 *
 **/

require ('./FilterableListBox.less')

import React from "react";
import {Search, ListBox, AbstractReactComponent, i18n} from "components";
import {Input, Button} from "react-bootstrap";
import {getSetFromIdsList} from "stores/app/utils.jsx";
var __FilterableListBox_timer = null

var FilterableListBox = class FilterableListBox extends AbstractReactComponent {
    constructor(props) {
        super(props)

        this.bindMethods('renderItemContent', 'handleCheckItem', 'handleSelectAll',
            'handleUnselectAll', 'handleSearch', 'handleSearchClear', 'handleSearchChange', 'focus')

        // Typ výběru:
        //   selectionType === 'selected', selectedIds obsahuje seznam vybraných id
        //   selectionType === 'unselected', selectedIds obsahuje seznam NEvybraných id

        const {selectionType, selectedIds} = props
        const supportInverseSelection = props.supportInverseSelection !== undefined ? props.supportInverseSelection : true
        var state = {
            selectionType: supportInverseSelection ? (selectionType || 'unselected') : 'selected',
            selectedIds: {},
            filterText: props.filterText || '',
            supportInverseSelection,
        }

        if (selectedIds) {
            state.selectedIds = getSetFromIdsList(selectedIds)
        }

        this.state = state
    }

    componentWillReceiveProps(nextProps) {
        if (typeof nextProps.selectedIds !== 'undefined') {
            let nextSelectedIds
            if (typeof nextProps.selectedIds !== 'undefined') {
                nextSelectedIds = getSetFromIdsList(nextProps.selectedIds)
            } else {
                nextSelectedIds = this.state.selectedIds
            }

            this.setState({
                selectedIds: nextSelectedIds,
                selectionType: nextProps.selectionType || this.state.selectionType,
                filterText: nextProps.filterText || this.state.filterText || '',
            })
        }
    }

    handleCheckItem(item) {
        const {selectionType, selectedIds} = this.state
        const {onChange} = this.props

        var newSelectedIds = {...selectedIds}
        var unselectedIds = []

        if (selectionType === 'selected') {
            if (selectedIds[item.id]) { // je označená, odznačíme ji
                delete newSelectedIds[item.id]
                unselectedIds.push(item.id)
            } else {    // není označená, označíme ji
                newSelectedIds[item.id] = true
            }
        } else {
            if (selectedIds[item.id]) { // je odznačená, odznačíme ji
                delete newSelectedIds[item.id]
                unselectedIds.push(item.id)
            } else {    // není odznačená, označíme ji
                newSelectedIds[item.id] = true
            }
        }

        this.setState({
            selectedIds: newSelectedIds
        })

        onChange && onChange(selectionType, Object.keys(newSelectedIds), unselectedIds, "TOGGLE_ITEM")
    }

    handleSelectAll() {
        const {onChange, items} = this.props
        const {supportInverseSelection} = this.state
        let type
        let selectedIds
        if (supportInverseSelection) {
            type = 'unselected'
            selectedIds = {}
        } else {
            type = 'selected'
            selectedIds = {}
            items.forEach(item => {
                selectedIds[item.id] = true
            })
        }

        this.setState({
            selectionType: type,
            selectedIds: selectedIds,
        })

        onChange && onChange(type, Object.keys(selectedIds), [], "SELECT_ALL")
    }

    handleUnselectAll() {
        const {onChange, items} = this.props
        const {supportInverseSelection, selectedIds} = this.state
        const type = 'selected'

        this.setState({
            selectionType: type,
            selectedIds: {},
        })

        var unselectedIds = []
        if (!supportInverseSelection) {
            items.forEach(item => {
                if (selectedIds[item.id]) {
                    unselectedIds.push(item.id)
                }
            })
        }

        onChange && onChange(type, [], unselectedIds, "UNSELECT_ALL")
    }

    renderItemContent(item, isActive) {
        const {selectionType, selectedIds} = this.state
        const checked = selectionType === 'selected' ? selectedIds[item.id] : !selectedIds[item.id]

        return (
            <div className='checkbox-item'>
                <Input type='checkbox' tabIndex={-1} checked={checked} label={item.name} onChange={(e) => {this.handleCheckItem.bind(this, item)()}}/>
            </div>
        )
    }

    handleSearchChange(e) {
        this.setState({
            filterText: e.target.value,
        }, ()=> {
            if (__FilterableListBox_timer) {
                clearTimeout(__FilterableListBox_timer)
            }
            __FilterableListBox_timer = setTimeout(this.handleSearch, 250);
        })
    }

    handleSearch() {
        const {filterText} = this.state
        const {onSearch} = this.props
        onSearch && onSearch(filterText)
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
        const {label, className, items, searchable, altSearch} = this.props
        const {filterText} = this.state
        const lbl = label ? <h4>{label}</h4> : null

        var cls = "filterable-listbox-container";
        if (className) {
            cls += " " + className;
        }

        return (
            <div className={cls}>
                {lbl}
                <div className="search-action-container">
                    {(searchable || altSearch) && <div className='search-container'>
                        {searchable && <Search
                            placeholder={i18n('search.input.search')}
                            filterText={filterText}
                            onChange={this.handleSearchChange}
                            onSearch={this.handleSearch}
                            onClear={this.handleSearchClear}
                        />}
                        {altSearch}
                    </div>}
                    <div className='actions-container'>
                        <Button bsStyle="link" onClick={this.handleSelectAll}>{i18n('global.title.selectAll')}</Button>
                        /
                        <Button bsStyle="link" onClick={this.handleUnselectAll}>{i18n('global.title.unselectAll')}</Button>
                    </div>
                </div>
                <div className='list-container'>
                    <ListBox
                        ref="listBox"
                        items={items}
                        renderItemContent={this.renderItemContent}
                        onCheck={this.handleCheckItem}
                    />
                </div>
            </div>
        )
    }
};

FilterableListBox.propsTypes = {
    supportInverseSelection: React.PropTypes.bool,
    selectedIds: React.PropTypes.array.isRequired,
    label:React.PropTypes.string,
    className:React.PropTypes.string,
    items: React.PropTypes.array.isRequired,
    searchable: React.PropTypes.bool,
    altSearch: React.PropTypes.object,
    onSearch: React.PropTypes.func,
    onChange: React.PropTypes.func,
    selectionType: React.PropTypes.string
};

module.exports = FilterableListBox;
