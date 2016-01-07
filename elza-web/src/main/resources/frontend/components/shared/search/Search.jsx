/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components'
 *
**/

import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {i18n} from 'components';
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
        this.handleChange = this.handleChange.bind(this);               // funckce pro aktualizaci hledaneho textu v komponentě

        this.state = {                                                  // inicializace stavu komponenty
            filterText: this.props.filterText,                          // hledaný text
        } 
    }

    handleSearch(e){
       this.props.onSearch(this.state.filterText);  
    }

    
    handleChange(e){
        this.setState({
            filterText: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });
        
    }

    render() {                          // metoda pro renderovani obsahu komponenty
        var cls = "search-container";   // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        var searchLabel = i18n('search.action.search');  
        return (
            <div className={cls}>
                <Input
                    type="text"
                    value={this.state.filterText}
                    ref="input"
                    labelClassName="label-class"
                    onChange={this.handleChange}                    
                />
                <Button onClick={this.handleSearch}>{searchLabel}</Button> 
            </div>
        );
    }
}


module.exports = Search;