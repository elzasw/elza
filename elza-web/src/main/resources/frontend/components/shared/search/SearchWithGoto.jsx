/**
 *  Komponenta hledání s možností "skákání" po výsledcích hledání. Je založená na komponentě Search s přidanými addons.
 */
import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {Search, i18n, Icon, NoFocusButton, AbstractReactComponent} from 'components';
import ReactDOM from 'react-dom'

require ('./SearchWithGoto.less');

var SearchWithGoto = class SearchWithGoto extends AbstractReactComponent {
    constructor(props) {
        super(props)

        this.bindMethods('handleFulltextChange', 'handleOnSearch')

        this.state = {
            filterText: props.filterText || '',
        }
    }

    componentWillReceiveProps(nexProps){
        if (typeof nexProps.filterText !== 'undefined') {
            this.setState({
                filterText: nexProps.filterText,
            })
        }
    }

    handleOnSearch(filterText, searchByEnter, shiftKey) {
        const {onFulltextNextItem, onFulltextPrevItem, searchedItems, filterCurrentIndex, showFilterResult, onFulltextSearch} = this.props

        if (searchByEnter) {    // při hledání pomocí enter se chováme jinak - pokud již něco vyledaného je, jdeme na další (případně předchozí) výsledek
            if (showFilterResult) { // je něco vyhledáno a nic mezitím nebylo změněno
                if (!shiftKey) {
                    if (filterCurrentIndex + 1 < searchedItems.length) {
                        onFulltextNextItem()
                    }
                } else {
                    if (filterCurrentIndex > 0) {
                        onFulltextPrevItem()
                    }
                }
            } else {
                onFulltextSearch()
            }
        } else {    // standardní hledání kliknutím na tlačítko hledat
            onFulltextSearch()
        }
    }

    handleFulltextChange(value) {
        const {onFulltextChange} = this.props;

        this.setState({
            filterText: value,
        }, () => {
            onFulltextChange && onFulltextChange(value)
        })
    }

    render() {
        const {searchedItems, filterCurrentIndex, showFilterResult, onFulltextSearch, onFulltextNextItem, onFulltextPrevItem} = this.props;
        const {filterText} = this.state;

        var actionAddons = []
        if (showFilterResult) {
            var searchedInfo
            if (searchedItems.length > 0) {
                searchedInfo = (
                    <div className='fa-tree-lazy-search-info'>
                        ({filterCurrentIndex + 1} z {searchedItems.length})
                    </div>
                )
            } else {
                searchedInfo = (
                    <div className='fa-tree-lazy-search-info'>
                        ({i18n('search.not.found')})
                    </div>
                )
            }

            if (searchedItems.length > 1) {
                var prevButtonEnabled = filterCurrentIndex > 0;
                var nextButtonEnabled = filterCurrentIndex < searchedItems.length - 1;

                actionAddons.push(<NoFocusButton disabled={!nextButtonEnabled} className="next" onClick={onFulltextNextItem}><Icon glyph='fa-chevron-down'/></NoFocusButton>)
                actionAddons.push(<NoFocusButton disabled={!prevButtonEnabled} className="prev" onClick={onFulltextPrevItem}><Icon glyph='fa-chevron-up'/></NoFocusButton>)
            }
            actionAddons.push(searchedInfo)
        }

        return (
            <Search
                placeholder={i18n('search.input.search')}
                filterText={filterText}
                onChange={e => this.handleFulltextChange(e.target.value)}
                onClear={e => {this.handleFulltextChange(''); onFulltextSearch(this.state.filterText)}}
                onSearch={this.handleOnSearch}
                actionAddons={actionAddons}
            />
        )
    }
}

SearchWithGoto.propTypes = {
    filterText: React.PropTypes.string,
    searchedItems: React.PropTypes.array.isRequired,
    filterCurrentIndex: React.PropTypes.number.isRequired,
    showFilterResult: React.PropTypes.bool.isRequired,
    onFulltextChange: React.PropTypes.func,
    onFulltextSearch: React.PropTypes.func,
    onFulltextNextItem: React.PropTypes.func,
    onFulltextPrevItem: React.PropTypes.func,
}

module.exports = SearchWithGoto