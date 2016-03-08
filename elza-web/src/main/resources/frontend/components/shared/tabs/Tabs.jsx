/**
 *  Panel záložek
 *
 *  Pro inicializaci staci naimportovat: import {Tabs} from 'components'
 *
**/

import React from 'react';
import ReactDOM from 'react-dom';
import {Nav, NavItem} from 'react-bootstrap';
import {ResizeStore} from 'stores';
import {AbstractReactComponent, Utils, Icon, i18n, NoFocusButton} from 'components';
var ShortcutsManager = require('react-shortcuts')
var Shortcuts = require('react-shortcuts/component')
var keyModifier = Utils.getKeyModifier()

require ('./Tabs.less');

var keymap = {
    Tabs: {
        prevTab: keyModifier + 'left',
        nextTab: keyModifier + 'right',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

/**
 *  Obalovací komponenta pro záložky a jejich obsah
 *  @param string className             třída komponenty
 *  $param obj children                 vnitřní části komponenty (přepínací panel, data zobrazovaná pod záložkami)
**/
var TabsContainer = class TabsContainer extends React.Component {
    constructor(props) {
        super(props);                   // volaní nadřazeného konstruktoru

        this.handleShortcuts = this.handleShortcuts.bind(this);
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts(action, e) {
        e.stopPropagation()
        e.preventDefault()

        var tabs
        for (var a=0; a<this.props.children.length; a++) {
            if (this.props.children[a].type === Tabs) {
                tabs = this.props.children[a]
                break
            }
        }
        if (!tabs) {
            console.error('First child of TabsContainer must be Tabs component!', 'Existing children', this.props.children)
            return
        }

        const {activeItem, items, onSelect} = tabs.props

        if (items.length === 0) {
            return
        }

        var index = 0
        for (var a=0; a<items.length; a++) {
            if (items[a] === activeItem) {
                index = a;
            }
        }

        switch (action) {
            case 'prevTab':
                if (index > 0) {
                    onSelect(items[index - 1])
                } else {
                    onSelect(items[items.length - 1])
                }
                break
            case 'nextTab':
                if (index + 1 < items.length) {
                    onSelect(items[index + 1])
                } else {
                    onSelect(items[0])
                }
                break
        }
    }

    render() {                          // metoda pro renderovani obsahu komponenty
        var cls = "tabs-container";     // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        return (
            <Shortcuts className={cls} name='Tabs' handler={this.handleShortcuts}>
                {this.props.children}  
            </Shortcuts>
        );
    }
}

TabsContainer.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
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

        const key = this.props.key || 'tab-content'

        return (
            <div key={key} className={cls}>
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
            var closeAction;
            var closeAction2;
            if (this.props.closable) {
                closeAction = <NoFocusButton title={closeTitle} onClick={this.handleTabClose.bind(this, item)}><Icon glyph="fa-times" /></NoFocusButton>
            }

            var closeTitle = i18n('tabs.action.closeTab');                                          // popisek ikony zavírající záložku
            var key = typeof item.key !== 'undefined' ? item.key : item.id;
            return <NavItem tabIndex={-1} key={key} ref={"tab"+i} eventKey={key}><span title={item.title}>{item.title}</span>
                <small>{item.desc}</small>
                {closeAction}{closeAction2}</NavItem>    // vlastni kod založky
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