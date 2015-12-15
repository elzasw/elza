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
        this.handleResize = this.handleResize.bind(this);               // funkce pro přenastavení komponenty po změně velikostí panelů v prohlížeči

        ResizeStore.listen(status => {
            this.handleResize();                                        // přenastavení komponenty po informaci od Store o změně velikosti panelů v prohlížeči
        });

        this.state = {                                                  // inicializace stavu komponenty
            tabPanelWidth: 0,                                           // šířka panelu záložek 
            tabWidth: 0,                                                // šířka jednotlivé záložky   
            tabDropdownWidth: 50,                                       // šířka rozbalovacího tlačítka se všemi záložkami  
        } 
    }

    /**
     *  Přenastavení stavu komponenty po změně velikosti panelů v prohlížeči
     *  Funce zjisti aktualni velikosti prvků a upraví jejich hodnotu ve state, 
     *  a tím spustí přerenderování komponenty
    **/
    handleResize() {   
        var el = ReactDOM.findDOMNode(this.refs.tabs);              // načtení objektu obalující záložky
        if(el){
            var tabPanelWidth = el.offsetWidth;                     // zjištění šířky objektu
        }else{
            var tabPanelWidth = 0;
        }
        var el = ReactDOM.findDOMNode(this.refs.tab0);              // načtení objektu první záložky
        if(this.state.tabWidth==0){                                 // šířka položky ještě nebyla zjištěna
            if(el){
                var tabWidth = el.offsetWidth;                      // zjištění šířky záložky
            }else{
                var tabWidth = 0;                                   // žádná záložka není
            }
        }else{
            var tabWidth = this.state.tabWidth;                     // šířka se nebude měnit (nastavi se puvodní)
        }
        this.setState({
            tabPanelWidth: tabPanelWidth,                           // uložení šířky panelu záložek do State 
            tabWidth: tabWidth,                                     // uložení šířky jednotlivé záložky do State
        });
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
    handleTabSelect(itemId) {       
        var item = this.props.items.one(i => {      
            if (i.id === itemId) {                  
                return i;                           
            } else {
                return null;                        
            }
        });
        this.props.onSelect(item);              // zobrazení vybrané položky                
    }

    /**
     *  Funkce spouštěná po vypsání okmponenty do skutečního DOMu stránky
    **/
    componentDidMount(){        
        this.handleResize();        // přepočtění velikostí elementů a přezenderování dle skutečných velikostí
    }

    /**
     *  Renderovánaí samotné komponenty přepínacích záložek
    **/
    render() {
        // zobrazované založky
        var tabs = this.props.items;

        var displayedTabs = tabs.map((item, i) => {                                                     // procházení všech záložek
            if(                                                                                         // zobrazení jen omezeného počtu zálozek (viz maxDisplayedTabs)
                (i+1)*this.state.tabWidth < (this.state.tabPanelWidth - this.state.tabDropdownWidth)    // pokud je jeste v panule žáložek pro záložku dost místa, bude přidána
            ){                                           
                var closeTitle = i18n('tabs.action.closeTab');                                          // popisek ikony zavírající záložku
                return <NavItem key={item.id} ref={"tab"+i} eventKey={item.id}><span>{item.title}</span><small>{item.desc}</small><Button title={closeTitle} onClick={this.handleTabClose.bind(this, item)}><Glyphicon glyph="remove" /></Button></NavItem>    // vlastni kod založky 
            }
        });
  
        // rozbalovaci seznam všech záložek 
        var allTabs = tabs.map((item) => {                                                              // procházení všech záložek
                var title = item.title || "Tab " + item.id;                                             // vytvoření popisku záložky
                return <NavItem key={item.id} eventKey={item.id}>{title}</NavItem>                                    // přidání záložky
        });
        if(tabs.length>0){
            allTabs = <NavDropdown pullRight title="" ref="tabDropdown">{allTabs}</NavDropdown>
        }else{
            allTabs = '';
        }

        // vrácení celé složené komponenty panelu záložek
        return (
            <Nav className="tabs-tabs-container" ref="tabs"  bsStyle="tabs" onSelect={this.handleTabSelect} activeKey={this.props.activeItem ? this.props.activeItem.id : null}>
                {displayedTabs}
                {allTabs}
            </Nav>
        )
    }
}

module.exports = {
    Container: TabsContainer,
    Tabs: Tabs,
    Content: TabContent,
}