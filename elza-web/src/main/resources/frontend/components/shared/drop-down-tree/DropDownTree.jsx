/**
 *  Rozbalovací strom položek
 *  Pro inicializaci staci naimportovat: import {Droptree} from 'components'
 *  Použití 
        <DropDownTree 
            items = {items} 
            value = {13}
            label = {"Vyberte si něco"}
            nullValue = {id: null, name: 'vychozi'}
            opened = {[10]}
            onSelect = {this.selectHandler}
        />
**/

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {Icon, i18n, AbstractReactComponent} from 'components';

import ReactDOM from 'react-dom'

require ('./DropDownTree.less');

/**
 *  Komponenta pro výběr z rozbalovacího stromu
 *  @param string className             třída komponenty
 *  $param string filterText            hledaný předvyplněný řezězec
**/
var DropDownTree = class DropDownTree extends AbstractReactComponent {
    constructor(props) {
        super(props);                                                   // volaní nadřazeného konstruktoru

        this.getOpenedDefault = this.getOpenedDefault.bind(this);       // funkce pro zjisteni uzlu, ktere maji byt automaticky otevrene
        this.isNodeOpened = this.isNodeOpened.bind(this);               // funkce pro kontrolu jestli je uzel otevřený
        this.getItemLabel = this.getItemLabel.bind(this);               // funkce zjisteni popisku (nazvu) vybrané položky

        this.handleItemSelect = this.handleItemSelect.bind(this);       // funkce po kliknutí pro výběr

        var opened = (props.opened ? props.opened : []);
        this.props.items.map((item, i) => {
            opened = opened.concat(this.getOpenedDefault(item)); 
        });
        var label = this.getItemLabel(this.props.value);
        this.state = {                                                  // inicializace stavu komponenty
            opened: opened,                                           // seznam rozbalenych uzlu
            label: (label != '' ? label : this.props.label),          // pokus je vybrany nejaká položka, vypíše se její název, jinak se vypíše defaultní popisek
            value : this.props.value                // id vybrane položky
        }
    }

    componentWillReceiveProps(nextProps) {
        var opened = (nextProps.opened ? nextProps.opened : []);
        var label = this.getItemLabel(nextProps.value);
        this.state = {                                                  // inicializace stavu komponenty
            opened: opened,                                           // seznam rozbalenych uzlu
            label: (label != '' ? label : nextProps.label),          // pokus je vybrany nejaká položka, vypíše se její název, jinak se vypíše defaultní popisek
            value : nextProps.value                // id vybrane položky
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
            label: item.name,
            value: item.id,
        });
        if (this.props.onSelect){
            this.props.onSelect(item.id);  
        }
    }

    handleOpenClose(e){
        var menu = $(e.target).closest(".dropDownTree").find(".menu");
        menu.toggle();

        if(this.props.value){
            var selectedItem = $(e.target).closest(".dropDownTree").find(".itemID"+this.state.value);
            if(selectedItem.length){
                selectedItem[0].scrollIntoView();
            }
        }

    }

    // metoda pro renderovani obsahu komponenty
    render() {
        const {nullValue, nullId, items, value} = this.props;

        var cls = "dropDownTree form-group";     // třída komponenty                 
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        
            var itemsData = [...items]
        if (nullValue){
            itemsData = [nullValue, ...itemsData];
        }

        var tree = itemsData.map((item) => {
            return this.renderNode(item, 1); 
        });

        var inputLabel;
        if (this.props.label) {
            inputLabel = <label className='control-label'><span>{this.props.label}</span></label>
        }
        var {refTables, ...other} = this.props;
        console.log(other);
        return (
            <div className={cls}>
                {inputLabel}
                <Button className='form-control' {...other} onClick={this.handleOpenClose.bind(this)}>
                    <div className='dropDownTree-label'>{this.state.label}</div>
                    <Icon glyph="fa-caret-down" />
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

        return  <li
                    key={item.id}
                    eventKey={item.id} 
                    className={"depth" + depth + " itemID" + item.id}
                >
                    <span className={"switcher " + (item.children && item.children.length>0 ? "enabled" : "") } onClick={this.handleToggleNode.bind(this,item)}>{item.children && item.children.length>0 ? (isOpened ? "−" : "+") : ''}</span>
                    <span 
                        className={"itemLabel " + (item.id==this.state.value ? "active" : "")} 
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
        if(item.id == this.props.value || opened.length>0){
            opened[opened.length]=item.id;
        }
        return opened;
    }

    //vrati popisek vybrane polozky
    getItemLabel(value){
        if (this.props.nullValue && this.props.nullValue.id == value) {
            return this.props.nullValue.name;
        }
        return this.getItemLabelInt(value, this.props.items);
    }

    //vrati popisek vybrane polozky
    getItemLabelInt(value, items) {
        for (var a=0; a<items.length; a++) {
            var item = items[a];
            if (item.id == value) {
                return item.name;
            }
            if (item.children) {
                var res = this.getItemLabelInt(value, item.children);
                if (res) {
                    return res;
                }
            }
        }
        return null;
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

DropDownTree.propTypes = {
    nullValue: React.PropTypes.object,
    items: React.PropTypes.array.isRequired,
    value: React.PropTypes.number,

}

module.exports =connect()( DropDownTree);