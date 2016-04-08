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
            showFilterResult: typeof props.showFilterResult !== 'undefined' ? props.showFilterResult : false,
        }
    }

    componentWillReceiveProps(nexProps){
        this.setState({
            filterText: typeof nexProps.filterText !== 'undefined' ? nexProps.filterText : this.state.filterText,
            showFilterResult: typeof nexProps.showFilterResult !== 'undefined' ? nexProps.showFilterResult : this.state.showFilterResult,
        })
    }

    handleOnSearch(filterText, searchByEnter, shiftKey) {
        const {onFulltextNextItem, onFulltextPrevItem, itemsCount, selIndex, showFilterResult, onFulltextSearch} = this.props

        if (searchByEnter) {    // při hledání pomocí enter se chováme jinak - pokud již něco vyledaného je, jdeme na další (případně předchozí) výsledek
            if (showFilterResult) { // je něco vyhledáno a nic mezitím nebylo změněno
                if (!shiftKey) {
                    if (selIndex + 1 < itemsCount) {
                        onFulltextNextItem()
                    }
                } else {
                    if (selIndex > 0) {
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
            showFilterResult: false,
        }, () => {
            onFulltextChange && onFulltextChange(value)
        })
    }

    render() {
        const {itemsCount, selIndex, onFulltextSearch, onFulltextNextItem, onFulltextPrevItem} = this.props;
        const {filterText, showFilterResult} = this.state;

        var actionAddons = []
        if (showFilterResult) {
            var searchedInfo
            if (itemsCount > 0) {
                searchedInfo = (
                    <div className='fa-tree-lazy-search-info'>
                        ({selIndex + 1} z {itemsCount})
                    </div>
                )
            } else {
                searchedInfo = (
                    <div className='fa-tree-lazy-search-info'>
                        ({i18n('search.not.found')})
                    </div>
                )
            }

            if (itemsCount > 1) {
                var prevButtonEnabled = selIndex > 0;
                var nextButtonEnabled = selIndex < itemsCount - 1;

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
    itemsCount: React.PropTypes.number.isRequired,
    selIndex: React.PropTypes.number.isRequired,
    showFilterResult: React.PropTypes.bool.isRequired,
    onFulltextChange: React.PropTypes.func,
    onFulltextSearch: React.PropTypes.func,
    onFulltextNextItem: React.PropTypes.func,
    onFulltextPrevItem: React.PropTypes.func,
}

module.exports = SearchWithGoto