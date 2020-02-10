/**
 *  Komponenta pro vyhledávání
 *
 *  Pro inicializaci staci naimportovat: import {Search} from 'components/index.jsx';
 *
**/

import React from 'react';
import ReactDOM from 'react-dom'

import './Search.less';
import FormInput from "../form/FormInput";
import NoFocusButton from "../button/NoFocusButton";
import Icon from "../icon/Icon";
import i18n from "../../i18n";

/**
 *  Komponenta pro vyhledávání
 *  @param string className             třída komponenty
 *  $param string filterText            hledaný předvyplněný řezězec
**/
class Search extends React.Component {
    constructor(props) {
        super(props);                                                   // volaní nadřazeného konstruktoru

        this.handleSearch = this.handleSearch.bind(this);               // funkce pro akci spoustící vyhledávání
        this.handleClear = this.handleClear.bind(this);
        this.handleChange = this.handleChange.bind(this);               // funckce pro aktualizaci hledaneho textu v komponentě
        this.handleKeyUp = this.handleKeyUp.bind(this);                 // funckce pro odchycení stisknutí klávesy enter a odeslání search
        this.getInput = this.getInput.bind(this);                 // funckce pro odchycení stisknutí klávesy enter a odeslání search

        this.state = {                                                  // inicializace stavu komponenty
            filterText: this.props.filterText || this.props.value,                          // hledaný text
        }
    }
    componentWillReceiveProps(nextProps){
        if (this.props.filterText !== nextProps.filterText || this.props.value !== nextProps.value) {
            this.setState({                                                  // inicializace stavu komponenty
                filterText: nextProps.filterText || nextProps.value || '',                          // hledaný text
            })
        }
    }

    handleSearch(searchByEnter, shiftKey){
       this.props.onSearch(this.state.filterText, searchByEnter, shiftKey);
    }

    handleClear(e){
        this.setState({
            filterText: null,
        })
        if (this.props) {
            this.props.onClear();
        }
    }

    handleKeyUp(e){
        const {textAreaInput} = this.props
        if (textAreaInput) {
            // u text area neřešíme
        } else {
            if (e.keyCode == 13){
                this.handleSearch(true, e.shiftKey);
            }
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
        const {disabled, textAreaInput, tabIndex, placeholder, extendedSearch, onClickExtendedSearch, extendedReadOnly, filter} = this.props;

        const readOnly = extendedSearch && extendedReadOnly;

        var cls = "search-container";   // třída komponenty
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        var afterInput
        var beforeInput
        if (this.props.afterInput) {
            afterInput = <div className='search-input-after'>{this.props.afterInput} </div>
        }
        if (this.props.beforeInput) {
            beforeInput = <div className='search-input-before'>{this.props.beforeInput} </div>
        }
        var searchLabel = i18n('search.action.search');

        var searchIcon = !filter ? <Icon glyph='fa-search'/> : <Icon glyph='fa-filter'/>; // Pokud je v props příznak filter změní se ikona

        var actions = []

        if (extendedSearch) {
            actions.push(<NoFocusButton key='search-extended' className="search-extendedsearch-extended" onClick={onClickExtendedSearch}><Icon glyph='fa-search-plus'/></NoFocusButton>)
        }

        if (!readOnly) {
            actions.push(<NoFocusButton disabled={disabled} key='handleSearch' className='search-button' onClick={this.handleSearch.bind(this, false, false)}>{searchIcon}</NoFocusButton>)
        }

        if (this.state.filterText) {
            actions.push(<NoFocusButton key='handleClear' className='clear-search-button' onClick={this.handleClear}><Icon glyph='fa-close'/></NoFocusButton>)
        }

        if (this.props.actionAddons) {
            actions = [...actions, this.props.actionAddons]
        }

        return (
            <div className={cls}>
                {beforeInput}
                <div className='search-input'>
                    {textAreaInput ? <FormInput
                        disabled={disabled}
                        componentClass='textarea'
                        tabIndex={tabIndex}
                        value={this.state.filterText}
                        ref="input"
                        placeholder={this.props.placeholder}
                        onChange={this.handleChange}
                        onKeyUp={this.handleKeyUp}
                    />:<FormInput
                        disabled={disabled}
                        type='text'
                        tabIndex={tabIndex}
                        value={this.state.filterText}
                        ref="input"
                        placeholder={this.props.placeholder}
                        readOnly={readOnly}
                        onChange={this.handleChange}
                        onKeyUp={this.handleKeyUp}
                    />}
                </div>
                <div className='search-actions'>
                    {actions}
                </div>
                {afterInput}
            </div>
        );
    }

    getInput() {
        return this.refs.input
    }
}


export default Search;
