/**
 *  Panel záložek
 *
 *  Pro inicializaci staci naimportovat: import {Tabs} from 'components'
 *
**/

import React from 'react';

import {NavDropdown, MenuItem, Button, DropdownButton, Glyphicon, Nav, NavItem} from 'react-bootstrap';
import {ResizeStore} from 'stores';
import {i18n} from 'components';
import ReactDOM from 'react-dom';

require ('./Tabs.less');

/**
 *  Obalovací komponenta pro záložky a jejich obsah
 *  @param string className             třída komponenty
 *  $param obj children                 vnitřní části komponenty (přepínací panel, data zobrazovaná pod záložkami)
**/
var TabsContainer = class TabsContainer extends React.Component {
    constructor(props) {
        super(props);                   // volaní nadřazeného konstruktoru
    }

    render() {                          // metoda pro renderovani obsahu komponenty
        var cls = "tabs-container";     // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        return (
            <div className={cls}>
                {this.props.children}  
            </div>
        );
    }
}


/**
 *  Komponena pro zobrazeni obshu dané záložky
 *  @param string className     třída komponenty
 *  $param obj children         datový obsah komponenty
**/
var TabContent = class TabContent extends React.Component {
    constructor(props) {
        super(props);                   // volání nadřazeného kontruktoru
    }

    render() {
        var cls = "tab-content";        // třída komponenty
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        return (
            <div className={cls}>
                {this.props.children}   
            </div>
        );
    }
}

/**
 *  Komponena pro panelů záložek pro přepínání
 *  @param string className             // třída komponenty
 *  $param obj children                 // vnitřní části komponenty (přepínací panel, data zobrazovaná pod záložkami)
**/
var Tabs = class Tabs extends React.Component {
    constructor(props) {
        super(props);                                                   // volání nadřazeného kontruktoru

        this.handleTabSelect = this.handleTabSelect.bind(this);         // funkce pro vybrání nové aktivní záložky
        this.handleTabClose = this.handleTabClose.bind(this);           // funkce pro zavření vybrané záložky
    }


    /**
     *  Zavření vybrané záložky
     *  @ param obj item        objekt záložky
     *  @ param event e         událost kliknutí na ikonu zavření záložky   
    **/
    handleTabClose(item, e) {
        this.props.onClose(item);       // zavření záložky

        e.preventDefault();             // zastavení původní akce vyvolané událostí
        e.stopPropagation();            // zastavení dalších akcí vázaných na tuto událost
    }

    /**
     *  Zobrazení obsahu vybrané záložky
     *  @ param int itemID      lokální identidikátor záložky 
    **/
    handleTabSelect(itemKey) {       
        var item = this.props.items.one(i => {      
            var key = typeof i.key !== 'undefined' ? i.key : i.id;
            if (key === itemKey) {                  
                return i;                           
            } else {
                return null;                        
            }
        });
        this.props.onSelect(item);              // zobrazení vybrané položky                
    }


    /**
     *  Renderovánaí samotné komponenty přepínacích záložek
    **/
    render() {
        var tabs = this.props.items.map((item, i) => {                                                              // vytvoření html seznamu všecch záložek                                        
            var closeTitle = i18n('tabs.action.closeTab');                                          // popisek ikony zavírající záložku
            var key = typeof item.key !== 'undefined' ? item.key : item.id;
            return <NavItem key={key} ref={"tab"+i} eventKey={key}><span>{item.title}</span><small>{item.desc}</small><Button title={closeTitle} onClick={this.handleTabClose.bind(this, item)}><Glyphicon glyph="remove" /></Button></NavItem>    // vlastni kod založky 
        });
  
        // vrácení html komponenty záložek
        var activeKey = null;
        if (this.props.activeItem) {
            activeKey = typeof this.props.activeItem.key !== 'undefined' ? this.props.activeItem.key : this.props.activeItem.id;
        }
        return (
            <Nav className="tabs-tabs-container" ref="tabs"  bsStyle="tabs" onSelect={this.handleTabSelect} activeKey={activeKey}>
                {tabs}
            </Nav>
        )
    }
}

module.exports = {
    Container: TabsContainer,
    Tabs: Tabs,
    Content: TabContent,
}