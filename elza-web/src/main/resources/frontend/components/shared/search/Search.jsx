/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components'
 *
**/

import React from 'react';

import {Button, Input} from 'react-bootstrap';
import {i18n} from 'components';

require ('./Search.less');

/**
 *  Komponenta pro vyhledávání
 *  @param string className             třída komponenty
 *  $param string filterText            hledaný řezězec
**/
var Search = class Search extends React.Component {
    constructor(props) {
        super(props);                   // volaní nadřazeného konstruktoru
    }

    handleSearch(){
        alert("aa");
    }

    handleChange(){
        //potreba zaktualizovat text inputu
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
                    value={this.props.filterText}
                    bsStyle="tab"
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