/**
 *  Komponenta hledání s možností "skákání" po výsledcích hledání. Je založená na komponentě Search s přidanými addons.
 */
import PropTypes from 'prop-types';

import React from 'react';

import './SearchWithGoto.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import NoFocusButton from '../button/NoFocusButton';
import Search from './Search';
import Icon from '../icon/Icon';
import i18n from '../../i18n';

class SearchWithGoto extends AbstractReactComponent {
    static propTypes = {
        filterText: PropTypes.string,
        itemsCount: PropTypes.number.isRequired,
        allItemsCount: PropTypes.number,
        textAreaInput: PropTypes.bool,
        selIndex: PropTypes.number,
        showFilterResult: PropTypes.bool,
        onFulltextChange: PropTypes.func,
        onFulltextSearch: PropTypes.func,
        onFulltextNextItem: PropTypes.func,
        onFulltextPrevItem: PropTypes.func,
        type: PropTypes.string.isRequired,
        extendedSearch: PropTypes.bool,
        extendedReadOnly: PropTypes.bool,
        onClickExtendedSearch: PropTypes.func,
        onClear: PropTypes.func,
    };

    static defaultProps = {
        type: 'GO_TO',
    };

    state = {
        filterText: this.props.filterText || '',
        showFilterResult: typeof this.props.showFilterResult !== 'undefined' ? this.props.showFilterResult : false,
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        var filterText = this.state.filterText;
        if (nextProps.filterText !== 'undefined' && nextProps.filterText !== this.props.filterText) {
            filterText = nextProps.filterText;
        }

        this.setState({
            filterText: filterText,
            showFilterResult:
                typeof nextProps.showFilterResult !== 'undefined'
                    ? nextProps.showFilterResult
                    : this.state.showFilterResult,
        });
    }

    handleOnSearch = (filterText, searchByEnter, shiftKey) => {
        const {
            type,
            onFulltextNextItem,
            onFulltextPrevItem,
            itemsCount,
            selIndex,
            showFilterResult,
            onFulltextSearch,
        } = this.props;

        switch (type) {
            case 'GO_TO':
                if (searchByEnter) {
                    // při hledání pomocí enter se chováme jinak - pokud již něco vyledaného je, jdeme na další (případně předchozí) výsledek
                    if (showFilterResult) {
                        // je něco vyhledáno a nic mezitím nebylo změněno
                        if (!shiftKey) {
                            if (selIndex + 1 < itemsCount) {
                                onFulltextNextItem();
                            }
                        } else {
                            if (selIndex > 0) {
                                onFulltextPrevItem();
                            }
                        }
                    } else {
                        onFulltextSearch(filterText);
                    }
                } else {
                    // standardní hledání kliknutím na tlačítko hledat
                    onFulltextSearch(filterText);
                }
                break;
            case 'INFO':
                onFulltextSearch(filterText);
                break;
            default:
                break;
        }
    };

    handleFulltextChange = value => {
        const {onFulltextChange} = this.props;

        this.setState(
            {
                filterText: value,
                showFilterResult: false,
            },
            () => {
                onFulltextChange && onFulltextChange(value);
            },
        );
    };

    handleClear = () => {
        const {onClear} = this.props;

        if(onClear){onClear()}
        this.handleFulltextChange('');
    };

    render() {
        const {
            type,
            itemsCount,
            allItemsCount,
            textAreaInput,
            placeholder,
            selIndex,
            onFulltextNextItem,
            onFulltextPrevItem,
            extendedSearch,
            onClickExtendedSearch,
            extendedReadOnly,
        } = this.props;
        const {filterText, showFilterResult} = this.state;

        const actionAddons = [];
        switch (type) {
            case 'GO_TO':
                if (showFilterResult) {
                    let searchedInfo;
                    if (itemsCount > 0) {
                        searchedInfo = (
                            <div key="info" className="fa-tree-lazy-search-info">
                                ({selIndex + 1} z {itemsCount})
                            </div>
                        );
                    } else {
                        searchedInfo = <div key="info" className="fa-tree-lazy-search-info">({i18n('search.not.found')})</div>;
                    }

                    if (itemsCount > 1) {
                        let prevButtonEnabled = selIndex > 0;
                        let nextButtonEnabled = selIndex < itemsCount - 1;

                        actionAddons.push(
                            <NoFocusButton key="next" disabled={!nextButtonEnabled} className="next" onClick={onFulltextNextItem}>
                                <Icon glyph="fa-chevron-down" />
                            </NoFocusButton>,
                        );
                        actionAddons.push(
                            <NoFocusButton key="prev" disabled={!prevButtonEnabled} className="prev" onClick={onFulltextPrevItem}>
                                <Icon glyph="fa-chevron-up" />
                            </NoFocusButton>,
                        );
                    }
                    actionAddons.push(searchedInfo);
                }
                break;
            case 'INFO':
                if (showFilterResult) {
                    let searchedText;
                    const filtered = this.props.filterText ? true : false;

                    if (filtered) {
                        if (itemsCount > 0) {
                            if (allItemsCount > 0 && itemsCount < allItemsCount) {
                                searchedText = i18n('search.found.more', itemsCount, allItemsCount);
                            } else {
                                searchedText = i18n('search.found', itemsCount);
                            }
                        } else {
                            searchedText = i18n('search.not.found');
                        }
                    } else {
                        if (allItemsCount > 0 && itemsCount < allItemsCount) {
                            searchedText = i18n('search.found.more', itemsCount, allItemsCount);
                        }
                    }
                    actionAddons.push(<div key="searched" className="fa-tree-lazy-search-info">{searchedText}</div>);
                }
                break;
            default:
                break;
        }

        return (
            <Search
                placeholder={placeholder || i18n('search.input.search')}
                textAreaInput={textAreaInput}
                filterText={filterText}
                onChange={e => this.handleFulltextChange(e.target.value)}
                onClear={this.handleClear}
                onSearch={this.handleOnSearch}
                actionAddons={actionAddons}
                extendedSearch={extendedSearch}
                onClickExtendedSearch={onClickExtendedSearch}
                extendedReadOnly={extendedReadOnly}
            />
        );
    }
}

export default SearchWithGoto;
