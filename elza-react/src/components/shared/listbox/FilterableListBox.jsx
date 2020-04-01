import './FilterableListBox.scss';
import PropTypes from 'prop-types';

import React from 'react';
import Search from '../search/Search';
import ListBox from './ListBox';
import i18n from '../../i18n';
import AbstractReactComponent from '../../AbstractReactComponent';
import {FormCheck} from 'react-bootstrap';
import {Button} from '../../ui';
import {getSetFromIdsList} from 'stores/app/utils.jsx';

/**
 *  ListBox komponenta s možností filtrování, hledání a označování.
 *
 **/

var __FilterableListBox_timer = null;

class FilterableListBox extends AbstractReactComponent {
    static propTypes = {
        supportInverseSelection: PropTypes.bool,
        selectedIds: PropTypes.array.isRequired,
        label: PropTypes.string,
        className: PropTypes.string,
        items: PropTypes.array.isRequired,
        searchable: PropTypes.bool,
        altSearch: PropTypes.object,
        onSearch: PropTypes.func,
        onChange: PropTypes.func,
        selectionType: PropTypes.string,
    };

    constructor(props) {
        super(props);

        this.bindMethods('renderItemContent', 'handleSearch', 'handleSearchClear', 'handleSearchChange', 'focus');

        // Typ výběru:
        //   selectionType === 'selected', selectedIds obsahuje seznam vybraných id
        //   selectionType === 'unselected', selectedIds obsahuje seznam NEvybraných id

        const {selectionType, selectedIds} = props;
        const supportInverseSelection =
            props.supportInverseSelection !== undefined ? props.supportInverseSelection : true;
        var state = {
            selectionType: supportInverseSelection ? selectionType || 'unselected' : 'selected',
            selectedIds: {},
            filterText: props.filterText || '',
            supportInverseSelection,
            rerender: {},
        };

        if (selectedIds) {
            state.selectedIds = getSetFromIdsList(selectedIds);
        }

        this.state = state;
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        if (typeof nextProps.selectedIds !== 'undefined') {
            let nextSelectedIds;
            if (typeof nextProps.selectedIds !== 'undefined') {
                nextSelectedIds = getSetFromIdsList(nextProps.selectedIds);
            } else {
                nextSelectedIds = this.state.selectedIds;
            }

            this.setState({
                selectedIds: nextSelectedIds,
                selectionType: nextProps.selectionType || this.state.selectionType,
                filterText: nextProps.filterText || this.state.filterText || '',
                rerender: {},
            });
        }
    }

    handleCheckItem = item => {
        const {selectionType, selectedIds} = this.state;
        const {onChange} = this.props;

        var newSelectedIds = {...selectedIds};
        var unselectedIds = [];

        let chekckedCount = 0;
        let unchekckedCount = 0;
        item.forEach(i => {
            if (selectedIds[i.id]) {
                chekckedCount++;
            } else {
                unchekckedCount++;
            }
        });
        if (selectionType === 'selected') {
            if (chekckedCount === item.length) {
                // všechny byly označené, všechny je odznačíme
                item.forEach(i => {
                    delete newSelectedIds[i.id];
                    unselectedIds.push(i.id);
                });
            } else {
                // minimálně jedna nebyla označená - všechny je označíme
                item.forEach(i => {
                    newSelectedIds[i.id] = true;
                });
            }
        } else {
            if (unchekckedCount === item.length) {
                // všechny byly označené, všechny je odznačíme
                item.forEach(i => {
                    newSelectedIds[i.id] = true;
                });
            } else {
                // minimálně jedna nebyla označená - všechny je označíme
                item.forEach(i => {
                    delete newSelectedIds[i.id];
                    unselectedIds.push(i.id);
                });
            }
        }

        onChange && onChange(selectionType, Object.keys(newSelectedIds), unselectedIds, 'TOGGLE_ITEM');

        this.setState({
            selectedIds: newSelectedIds,
            rerender: {},
        });
    };

    handleSelectAll = () => {
        const {onChange, items} = this.props;
        const {supportInverseSelection} = this.state;
        let type;
        let selectedIds;
        if (supportInverseSelection) {
            type = 'unselected';
            selectedIds = {};
        } else {
            type = 'selected';
            selectedIds = {};
            items.forEach(item => {
                selectedIds[item.id] = true;
            });
        }

        this.setState({
            selectionType: type,
            selectedIds: selectedIds,
            rerender: {},
        });

        onChange && onChange(type, Object.keys(selectedIds), [], 'SELECT_ALL');
    };

    handleUnselectAll = () => {
        const {onChange, items} = this.props;
        const {supportInverseSelection, selectedIds} = this.state;
        const type = 'selected';

        this.setState({
            selectionType: type,
            selectedIds: {},
            rerender: {},
        });

        let unselectedIds = [];
        if (!supportInverseSelection) {
            items.forEach(item => {
                if (selectedIds[item.id]) {
                    unselectedIds.push(item.id);
                }
            });
        }

        onChange && onChange(type, [], unselectedIds, 'UNSELECT_ALL');
    };

    renderItemContent(props, onCheckItem) {
        const {item, index} = props;
        const {selectionType, selectedIds} = this.state;
        const checked = selectionType === 'selected' ? !!selectedIds[item.id] : !selectedIds[item.id];

        return (
            <div className="checkbox-item">
                <FormCheck tabIndex={-1} checked={checked} onMouseDown={onCheckItem}></FormCheck>
                {item.name}
            </div>
        );
    }

    handleSearchChange(e) {
        this.setState(
            {
                filterText: e.target.value,
                rerender: {},
            },
            () => {
                if (__FilterableListBox_timer) {
                    clearTimeout(__FilterableListBox_timer);
                }
                __FilterableListBox_timer = setTimeout(this.handleSearch, 250);
            },
        );
    }

    handleSearch() {
        const {filterText} = this.state;
        const {onSearch} = this.props;
        onSearch && onSearch(filterText);
    }

    handleSearchClear() {
        this.setState(
            {
                filterText: '',
                rerender: {},
            },
            () => this.handleSearch(),
        );
    }

    focus() {
        this.refs.listBox.focus();
    }

    render() {
        const {label, className, items, searchable, altSearch} = this.props;
        const {filterText, rerender} = this.state;
        const lbl = label ? <h4>{label}</h4> : null;

        var cls = 'filterable-listbox-container';
        if (className) {
            cls += ' ' + className;
        }

        return (
            <div className={cls}>
                {lbl}
                <div className="search-action-container">
                    {(searchable || altSearch) && (
                        <div className="search-container">
                            {searchable && (
                                <Search
                                    placeholder={i18n('search.input.search')}
                                    filterText={filterText}
                                    onChange={this.handleSearchChange}
                                    onSearch={this.handleSearch}
                                    onClear={this.handleSearchClear}
                                />
                            )}
                            {altSearch}
                        </div>
                    )}
                    <div className="actions-container">
                        <Button variant="link" onClick={this.handleSelectAll}>
                            {i18n('global.title.selectAll')}
                        </Button>
                        /
                        <Button variant="link" onClick={this.handleUnselectAll}>
                            {i18n('global.title.unselectAll')}
                        </Button>
                    </div>
                </div>
                <div className="list-container">
                    {this.props.children}
                    <ListBox
                        multiselect
                        ref="listBox"
                        rerender={rerender}
                        items={items}
                        renderItemContent={this.renderItemContent}
                        onCheck={this.handleCheckItem}
                    />
                </div>
            </div>
        );
    }
}

export default FilterableListBox;
