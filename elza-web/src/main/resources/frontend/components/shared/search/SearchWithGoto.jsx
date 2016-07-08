/**
 *  Komponenta hledání s možností "skákání" po výsledcích hledání. Je založená na komponentě Search s přidanými addons.
 */
import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {Search, i18n, Icon, NoFocusButton, AbstractReactComponent} from 'components/index.jsx';
import ReactDOM from 'react-dom'

require ('./SearchWithGoto.less');

var SearchWithGoto = class SearchWithGoto extends AbstractReactComponent {
    constructor(props) {
        super(props)

        this.bindMethods('handleFulltextChange', 'handleOnSearch', 'handleClear')

        this.state = {
            filterText: props.filterText || '',
            showFilterResult: typeof props.showFilterResult !== 'undefined' ? props.showFilterResult : false,
        }
    }

    componentWillReceiveProps(nextProps){
        var filterText = this.state.filterText
        if (nextProps.filterText !== 'undefined' && nextProps.filterText !== this.props.filterText) {
            filterText = nextProps.filterText
        }
        
        this.setState({
            filterText: filterText,
            showFilterResult: typeof nextProps.showFilterResult !== 'undefined' ? nextProps.showFilterResult : this.state.showFilterResult,
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
                onFulltextSearch(filterText)
            }
        } else {    // standardní hledání kliknutím na tlačítko hledat
            onFulltextSearch(filterText)
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

    handleClear() {
        const {onFulltextSearch} = this.props

        this.handleFulltextChange('');
        onFulltextSearch('')
    }

    render() {
        const {itemsCount, textAreaInput, placeholder, selIndex, onFulltextNextItem, onFulltextPrevItem} = this.props;
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
                placeholder={placeholder || i18n('search.input.search')}
                textAreaInput={textAreaInput}
                filterText={filterText}
                onChange={e => this.handleFulltextChange(e.target.value)}
                onClear={this.handleClear}
                onSearch={this.handleOnSearch}
                actionAddons={actionAddons}
            />
        )
    }
}

SearchWithGoto.propTypes = {
    filterText: React.PropTypes.string,
    itemsCount: React.PropTypes.number.isRequired,
    textAreaInput: React.PropTypes.bool,
    selIndex: React.PropTypes.number.isRequired,
    showFilterResult: React.PropTypes.bool.isRequired,
    onFulltextChange: React.PropTypes.func,
    onFulltextSearch: React.PropTypes.func,
    onFulltextNextItem: React.PropTypes.func,
    onFulltextPrevItem: React.PropTypes.func,
}

module.exports = SearchWithGoto