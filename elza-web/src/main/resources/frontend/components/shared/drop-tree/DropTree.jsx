/**
 *  Rozbalovací strom položek
 *  Pro inicializaci staci naimportovat: import {Droptree} from 'components'
 *
**/

import React from 'react';

import {DropdownButton, MenuItem} from 'react-bootstrap';
import ReactDOM from 'react-dom';

require ('./DropTree.less');

/**
 *  Obalovací komponenta Drop Tree
 *  @param string className             třída komponenty
 *  $param obj children                 vnitřní části komponenty (přepínací panel, data zobrazovaná pod záložkami)
**/
var DropTree = class DropTree extends React.Component {
    constructor(props) { 
        super(props);                   // volaní nadřazeného konstruktoru;
    }

    /*
    handleToggleNode(itemId, e) {       
        e.preventDefault();
        var opened = [];                                                        // novy seznam rozbalenych uzlu
        var alreadyOpened = false;                                              // priznak jestli vybrany uzel je otevreny
        for(var i; i<this.state.opened.length; i++){                            // nejdrive prijdu vsechny otevrene uzly, abych zjistil jestli je vybrany uzel otevreny
            if(this.state.opened[i] == itemId){
               alreadyOpened = true;                                            // vybrany uzel uz je ovevreny
            }else{
                opened[opened.length] = this.state.opened[i];                   // seznam otevrenych uzlu bez vybraneho uzlu
            }
        }
        if(!alreadyOpened){                                                     // pokud uzel nebyl otevreny
            opened[opened.length] = itemId;                                     // přidá se do seznamu otevřených uzlů 
        }                                                                       // pokud už otevřený byl, zůstává nás seznam otevřených uzlů bez vybraného získaných v předchozím cyklu
        this.setState({"opened": opened});                                      // novy seznam uzlu se uloží do state> dojde k prerenderovani                              
    }
    */

    handleItemSelect(item, e){
        e.preventDefault();
        console.log("vybrano " + item.name);
    }

    handleOpenClose(e){
        e.preventDefault();
        e.stopPropagation(); 
        var menu = $(e.target).closest(".dropDownTree").find(".dropdown-menu");
        var selectedItem = $(e.target).closest(".dropDownTree").find(".itemID"+this.props.selectedItemID);
        menu.show();
        selectedItem[0].scrollIntoView();
        alert("aa");
    }

    render() {                          // metoda pro renderovani obsahu komponenty
        console.log(this.props.selectedItemID);
        var cls = "dropTree-container";     // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        var tree = this.props.items.map((item, i) => {   
                return this.renderNode(item, 1); 
        });
        return (
            <div className={"dropDownTree"}>
                <DropdownButton  title={"AA"} onClick={this.handleOpenClose.bind(this)}>
                    {tree}
                </DropdownButton>
            </div>
        );
    }
    renderNode(item, depth){
        var nodes = []
        nodes[0] = <MenuItem 
                eventKey="{item.id}" 
                className={(item.id==this.props.selectedItemID ? "active" : "") + " depth" + depth + " itemID" + item.id}
                onClick={this.handleItemSelect.bind(this,item)}
            >   
                {item.name}
            </MenuItem>;
        if(item.childrens && item.childrens.length>0){
            for(var i = 0; i<item.childrens.length; i++){
                nodes[nodes.length] = this.renderNode(item.childrens[i], depth+1);
            };
        }
        return nodes;
    }
}

module.exports = DropTree;