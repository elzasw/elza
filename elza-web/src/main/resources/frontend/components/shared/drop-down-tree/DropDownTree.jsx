/**
 *  Rozbalovací strom položek
 *  Pro inicializaci staci naimportovat: import {Droptree} from 'components/index.jsx';
 *  Použití 
        <DropDownTree 
            items = {items} 
            value = {13}
            label = {"Vyberte si něco"}
            nullValue = {id: null, name: 'vychozi'}
            opened = {[10]}
            onChange = {this.selectHandler}
        />
**/

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {Icon, i18n, AbstractReactComponent} from 'components/index.jsx';
import {getBootstrapInputComponentInfo} from 'components/form/FormUtils.jsx';

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
        this.getFirstPossibleRecordType = this.getFirstPossibleRecordType.bind(this);
        this.preselectValue = this.preselectValue.bind(this);

        var opened = (props.opened ? props.opened : []);
        this.props.items.map((item, i) => {
            opened = opened.concat(this.getOpenedDefault(item));
        });
        var label = this.getItemLabel(this.props.value, this.props.items);
        this.state = {                                                  // inicializace stavu komponenty
            opened: opened,                                           // seznam rozbalenych uzlu
            label: (label != '' ? label : this.props.label),          // pokus je vybrany nejaká položka, vypíše se její název, jinak se vypíše defaultní popisek
            value : this.props.value                // id vybrane položky
        }
    }

    componentWillReceiveProps(nextProps) {
        var label = this.getItemLabel(nextProps.value, nextProps.items);
        if (nextProps.preselect) {
             this.preselectValue(nextProps);
        }else{
            this.setState({
                label: (label != '' ? label : nextProps.label),          // pokus je vybrany nejaká položka, vypíše se její název, jinak se vypíše defaultní popisek
                value: nextProps.value                // id vybrane položky
            })
            if (nextProps.onChange && this.props.value !== nextProps.value){
                nextProps.onChange(nextProps.value);
            }

        }

    }

    componentDidMount(){

        if (this.props.value) {
            this.setState({"value": this.props.value});
            if (this.props.onChange){
                this.props.onChange(this.props.value);
            }
        }else {
            this.preselectValue(this.props);
        }
    }

    preselectValue(nextProps) {
        if (nextProps.value == undefined) {
            var preselect = this.getFirstPossibleRecordType(nextProps.items);
            if (preselect.found) {
                var label = this.getItemLabel(preselect.found.id, nextProps.items);
                this.setState({
                    label: label,
                    value: preselect.found.id
                });

                if (nextProps.onChange) {
                    nextProps.onChange(preselect.found.id, preselect.found);
                }
            }
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
        if (this.props.onChange){
            this.props.onChange(item.id, item);  
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

    /**
     * Projde celou hierarchii a najde první typ, pod který lze vložit novou osobu.
     * @param recordTypes typy rejstříků
     * @returns {*}
     */
    getFirstPossibleRecordType(recordTypes) {
        if (this.props.nullValue) {
            return {
                found: null,
                opened: null
            }
        }
        var opened = [];
        if (!recordTypes) {
            return {
                found: null,
                opened: null
            }
        }

        var loop = (type, opened) => {
            if (type.addRecord) {
                return type;
            }

            if (type.children && type.children.length > 0) {
                for (var key in type.children) {
                    var found = loop(type.children[key], opened);
                    if (found) {
                        opened.push(type.children[key].id);
                        return found;
                    }
                }
            }
            return null;
        }

        var found = null;
        for (var key in recordTypes) {
            var found = loop(recordTypes[key], opened);

            if (found) {
                opened.push(recordTypes[key].id);
                break;
            }
        }

        return {
            found: found,
            opened: opened
        }
    }

    // metoda pro renderovani obsahu komponenty
    render() {
        const {nullValue, nullId, items, value} = this.props;

        var bootInfo = getBootstrapInputComponentInfo(this.props);

        var cls = bootInfo.cls + " dropDownTree";     // třída komponenty                 
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

        var {refTables, ...other} = this.props;
        return (
            <div className={cls}>
                {this.props.label && <label className='control-label'>{this.props.label}</label>}
                <Button
                    className='form-control'
                    onClick={this.handleOpenClose.bind(this)}
                    onFocus={this.props.onFocus}
                    onBlur={this.props.onBlur}
                    disabled={this.props.disabled}
                >
                    <div className='dropDownTree-label'>{this.state.label}</div>
                    <Icon glyph="fa-caret-down" />
                </Button>
                <ul className={"menu"}>
                    {tree}
                </ul>
                {this.props.hasFeedback && <span className={'glyphicon form-control-feedback glyphicon-' + bootInfo.feedbackIcon}></span>}
                {this.props.help && <span className='help-block'>{this.props.help}</span>}
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
        var addClassName = '';
        var clickEvent = this.handleItemSelect.bind(this,item);

        if (this.props.addRegistryRecord && item.addRecord === false){
            addClassName = ' unavailable';
            clickEvent = null;
        }

        return  <li
                    key={item.id}
                    eventKey={item.id} 
                    className={"depth" + depth + " itemID" + item.id + addClassName}
                >
                    <span className={"switcher " + (item.children && item.children.length>0 ? "enabled" : "") } onClick={this.handleToggleNode.bind(this,item)}>{item.children && item.children.length>0 ? (isOpened ? "−" : "+") : ''}</span>
                    <span 
                        className={"itemLabel " + (item.id==this.state.value ? "active" : "")} 
                        onClick={clickEvent}
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
    getItemLabel(value, items){
        if (this.props.nullValue && this.props.nullValue.id == value) {
            return this.props.nullValue.name;
        }
        return this.getItemLabelInt(value, items);
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
    value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),

}

module.exports =connect()( DropDownTree);