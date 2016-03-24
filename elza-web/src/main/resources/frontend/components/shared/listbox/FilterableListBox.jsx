/**
 *  ListBox komponenta s možností filtrování a hledání.
 *
 **/

require ('./FilterableListBox.less')

import React from 'react';
import {Search, ListBox, AbstractReactComponent, i18n} from 'components';
import {Input, Button} from 'react-bootstrap';
import ReactDOM from 'react-dom';

var __FilterableListBox_timer = null

var FilterableListBox = class FilterableListBox extends AbstractReactComponent {
    constructor(props) {
        super(props)

        this.bindMethods('renderItemContent', 'handleCheckItem', 'handleSelectAll',
            'handleUnselectAll', 'handleSearch', 'handleSearchClear', 'handleSearchChange')

        // Typ výběru:
        //   selectionType === 'selected', selectedIds obsahuje seznam vybraných id
        //   selectionType === 'unselected', selectedIds obsahuje seznam NEvybraných id

        this.state = {
            selectionType: 'unselected',
            selectedIds: {},
            filterText: ''
        }
    }

    handleCheckItem(item) {
        const {selectionType, selectedIds} = this.state
        const {onChange} = this.props

        var newSelectedIds = {...selectedIds}

        if (selectionType === 'selected') {
            if (selectedIds[item.id]) { // je označená, odznačíme ji
                delete newSelectedIds[item.id]
            } else {    // není označená, označíme ji
                newSelectedIds[item.id] = true
            }
        } else {
            if (selectedIds[item.id]) { // je odznačená, označíme ji
                delete newSelectedIds[item.id]
            } else {    // není odznačená, označíme ji
                newSelectedIds[item.id] = true
            }
        }

        this.setState({
            selectedIds: newSelectedIds
        })

        onChange && onChange(selectionType, Object.keys(newSelectedIds))
    }

    handleSelectAll() {
        const {onChange} = this.props
        const type = 'unselected'

        this.setState({
            selectionType: type,
            selectedIds: {},
        })

        onChange && onChange(type, [])
    }

    handleUnselectAll() {
        const {onChange} = this.props
        const type = 'selected'

        this.setState({
            selectionType: type,
            selectedIds: {},
        })

        onChange && onChange(type, [])
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

    render() {
        const {label, className, items, searchable} = this.props
        const {filterText} = this.state
        const lbl = label ? <h4>{label}</h4> : null

        var cls = "filterable-listbox-container";
        if (className) {
            cls += " " + className;
        }

        return (
            <div className={cls}>
                {lbl}
                <div className='actions-container'>
                    <Button bsStyle="link" onClick={this.handleSelectAll}>{i18n('global.title.selectAll')}</Button>
                    /
                    <Button bsStyle="link" onClick={this.handleUnselectAll}>{i18n('global.title.unselectAll')}</Button>
                </div>
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
                        items={items}
                        renderItemContent={this.renderItemContent}
                        onCheck={this.handleCheckItem}
                    />
                </div>
            </div>
        )
    }
}

module.exports = FilterableListBox
