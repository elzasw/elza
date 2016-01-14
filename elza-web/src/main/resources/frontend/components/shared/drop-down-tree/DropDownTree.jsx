/**
 *  Rozbalovací strom položek
 *  Pro inicializaci staci naimportovat: import {Droptree} from 'components'
 *  Použití 
        <DropDownTree 
            items = {items} 
            selectedItemID = {13}
            label = {"Vyberte si něco"}
            null = {"Pro hodnotu null"} // text ktery se zobrazi
            nullValue = {null} // hodnota která bude předána. default null pokud je definovano null
            opened = {[10]}
            onSelect = {this.selectHandler}
        />
**/

import React from 'react';

import {Button, Glyphicon} from 'react-bootstrap';
import {i18n} from 'components';
import ReactDOM from 'react-dom'

require ('./DropDownTree.less');

/**
 *  Komponenta pro výběr z rozbalovacího stromu
 *  @param string className             třída komponenty
 *  $param string filterText            hledaný předvyplněný řezězec
**/
var DropDownTree = class DropDownTree extends React.Component {
    constructor(props) {
        super(props);                                                   // volaní nadřazeného konstruktoru

        this.nullValue = undefined;
        this.nullId = null;

        this.getOpenedDefault = this.getOpenedDefault.bind(this);       // funkce pro zjisteni uzlu, ktere maji byt automaticky otevrene
        this.isNodeOpened = this.isNodeOpened.bind(this);               // funkce pro kontrolu jestli je uzel otevřený
        this.getItemLabel = this.getItemLabel.bind(this);               // funkce zjisteni popisku (nazvu) vybrané položky
        this.handleItemSelect = this.handleItemSelect.bind(this);       // funkce po kliknutí pro výběr

        if (this.props.nullValue !== undefined){
            this.nullValue = this.props.nullValue;
            if (props.nullId !== undefined) {
                this.nullId = props.nullId;
            }
        }
        var opened = (props.opened ? props.opened : []);
        this.props.items.map((item, i) => {   
            opened = opened.concat(this.getOpenedDefault(item)); 
        });
        var label = this.getItemLabel(this.props.selectedItemID);
        this.state = {                                                  // inicializace stavu komponenty
            "opened": opened,                                           // seznam rozbalenych uzlu  
            "label": (label != '' ? label : this.props.label),          // pokus je vybrany nejaká položka, vypíše se její název, jinak se vypíše defaultní popisek
            "selectedItemID" : this.props.selectedItemID                // id vybrane položky     
        }
    }

    handleToggleNode(item, e) { 
        e.preventDefault();
        var opened = [];                                                        // novy seznam rozbalenych uzlu
        var alreadyOpened = false;                                              // priznak jestli vybrany uzel je otevreny
        for(var i=0; i<this.state.opened.length; i++){                          // nejdrive prijdu vsechny otevrene uzly, abych zjistil jestli je vybrany uzel otevreny
            if(this.state.opened[i] == item.id){
               alreadyOpened = true;                                            // vybrany uzel uz je ovevreny
            }else{
                opened[opened.length] = this.state.opened[i];                   // seznam otevrenych uzlu bez vybraneho uzlu
            }
        }
        if(!alreadyOpened){                                                     // pokud uzel nebyl otevreny
            opened[opened.length] = item.id;                                     // přidá se do seznamu otevřených uzlů 
        }                                                                       // pokud už otevřený byl, zůstává nás seznam otevřených uzlů bez vybraného získaných v předchozím cyklu
        this.setState({"opened": opened});                                      // novy seznam uzlu se uloží do state> dojde k prerenderovani   
    }

    handleItemSelect(item, e){
        this.handleOpenClose(e);
        this.setState({
            "label": this.getItemLabel(item.id),
            "selectedItemID": item.id,
        });
        if(this.props.onSelect){
            this.props.onSelect(item.id);  
        }
    }

    handleOpenClose(e){
        var menu = $(e.target).closest(".dropDownTree").find(".menu");
        menu.toggle();

        if(this.props.selectedItemID){
            var selectedItem = $(e.target).closest(".dropDownTree").find(".itemID"+this.state.selectedItemID);
            if(selectedItem.length){
                selectedItem[0].scrollIntoView();
            }
        }

    }
// chybí nastavit label
    // metoda pro renderovani obsahu komponenty
    render() {     
        var cls = "dropTree-container";     // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }

        if (this.nullValue !== undefined){
            this.props.items.unshift({id: this.nullId, name: this.nullValue});
        }

        var tree = this.props.items.map((item) => {   
                return this.renderNode(item, 1); 
        });
        return (
            <div className={"dropDownTree"}>
                <Button onClick={this.handleOpenClose.bind(this)}>
                    {this.state.label}
                    <Glyphicon glyph="triangle-bottom" />
                </Button>
                <ul className={"menu"}>
                    {tree}
                </ul>
            </div>
        );
          
    }
    renderNode(item, depth){
        var subList = [];
        var isOpened = this.isNodeOpened(item.id);
        if(item.children && item.children.length>0 && isOpened){
            for(var i = 0; i<item.children.length; i++){
                subList[subList.length] = this.renderNode(item.children[i], depth+1);
            };
        }

console.log(this.state.selectedItemID, item.id);

        return  <li
                    key={item.id}
                    eventKey={item.id} 
                    className={"depth" + depth + " itemID" + item.id}
                >
                    <span className={"switcher " + (item.children && item.children.length>0 ? "enabled" : "") } onClick={this.handleToggleNode.bind(this,item)}>{item.children && item.children.length>0 ? (isOpened ? "−" : "+") : ''}</span>
                    <span 
                        className={"itemLabel " + (item.id==this.state.selectedItemID ? "active" : "")} 
                        onClick={this.handleItemSelect.bind(this,item)}
                    >
                        {item.name}
                    </span>
                    { subList.length>0 ? <ul>{subList}</ul> : ""}    
                </li>
    }

    //nastavi vsehny nadrazene uzly vybrane polozce jako otevrene - aby byla vybrana polozka vyiditelna
    getOpenedDefault(item){
        var opened = [];
        if(item.children && item.children.length>0){
            for(var i = 0; i<item.children.length; i++){
                opened = opened.concat(this.getOpenedDefault(item.children[i]));
            };
        }
        if(item.id == this.props.selectedItemID || opened.length>0){
            opened[opened.length]=item.id;
        }
        return opened;
    }

    //vrati popisek vybrane polozky
    getItemLabel(selectedItemID, item = false){
        var label = '';
        if(!item){
            this.props.items.map((item, i) => {  
                var childLabel = this.getItemLabel(selectedItemID, item);
                if(childLabel != ''){
                   label = childLabel;
                }
            });
        }else{
            if(item.children && item.children.length>0){
                for(var i = 0; i<item.children.length; i++){
                    var childLabel = this.getItemLabel(selectedItemID, item.children[i])
                    if(childLabel != ''){
                        label = childLabel;
                    };
                };
            }
        }
        if(item.id == selectedItemID){
            label = item.name;
        }
        return label;
    }

    isNodeOpened(nodeID){
        for(var i = 0; i<= this.state.opened.length; i++){
            if(this.state.opened[i] == nodeID){
                return true;
            }
        }
        return false;
    }

}


module.exports = DropDownTree;