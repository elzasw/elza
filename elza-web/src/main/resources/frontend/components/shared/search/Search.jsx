/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components'
 *
**/

import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {i18n, Icon} from 'components';
import ReactDOM from 'react-dom'

require ('./Search.less');

/**
 *  Komponenta pro vyhledávání
 *  @param string className             třída komponenty
 *  $param string filterText            hledaný předvyplněný řezězec
**/
var Search = class Search extends React.Component {
    constructor(props) {
        super(props);                                                   // volaní nadřazeného konstruktoru

        this.handleSearch = this.handleSearch.bind(this);               // funkce pro akci spoustící vyhledávání
        this.handleClear = this.handleClear.bind(this);
        this.handleChange = this.handleChange.bind(this);               // funckce pro aktualizaci hledaneho textu v komponentě
        this.handleKeyUp = this.handleKeyUp.bind(this);                 // funckce pro odchycení stisknutí klávesy enter a odeslání search

        this.state = {                                                  // inicializace stavu komponenty
            filterText: this.props.filterText,                          // hledaný text
        }
    }
    componentWillReceiveProps(nexProps){
        this.state = {                                                  // inicializace stavu komponenty
            filterText: nexProps.filterText,                          // hledaný text
        }
    }

    handleSearch(e){
       this.props.onSearch(this.state.filterText);
    }

    handleClear(e){
        this.state = {
            filterText: null,
        }
        if (this.props) {
            this.props.onClear();
        }
    }

    handleKeyUp(e){
        if (e.keyCode == 13){
            this.handleSearch(e);
        }
    }

    
    handleChange(e){
        this.setState({
            filterText: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });
        if (this.props.onChange) {
            this.props.onChange(e);
        }
    }

    render() {                          // metoda pro renderovani obsahu komponenty
        var cls = "search-container";   // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        var afterInput = '';
        var beforeInput = '';
        if (this.props.afterInput) {
            afterInput = <div className='search-input-after'>{this.props.afterInput} </div>
        }
        if (this.props.beforeInput) {
            beforeInput = <div className='search-input-before'>{this.props.beforeInput} </div>
        }
        var searchLabel = i18n('search.action.search');
        var clearButton = '';
        if(this.state.filterText)
            clearButton = <div className='clear-search-button'><Button onClick={this.handleClear}><Icon glyph='fa-close'/></Button></div>;
        return (
            <div className={cls}>
                {beforeInput}
                <div className='search-input'>
                    <Input
                        type="text"
                        value={this.state.filterText}
                        ref="input"
                        labelClassName="label-class"
                        placeholder={this.props.placeholder}
                        onChange={this.handleChange}
                        onKeyUp={this.handleKeyUp}
                    />
                    <div className='search-button'><Button onClick={this.handleSearch}><Icon glyph='fa-search'/></Button></div>
                    {clearButton}

                </div>
                {afterInput}
            </div>
        );
    }
}


module.exports = Search;