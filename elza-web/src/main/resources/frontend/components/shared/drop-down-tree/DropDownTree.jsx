/**
 *  Rozbalovací strom položek
 *  Pro inicializaci staci naimportovat: import {Droptree} from 'components/index.jsx';
 *  Použití 
        <DropDownTree 
            items = {items} 
            value = {13}
            label = {'Vyberte si něco'}
            nullValue = {id: null, name: 'vychozi'}
            opened = {[10]}
            onChange = {this.selectHandler}
        />
**/

import React from 'react';
//import ReactDOM from 'react-dom';
import {Button} from 'react-bootstrap';
import {Icon, i18n, AbstractReactComponent} from 'components/index.jsx';
import {getBootstrapInputComponentInfo} from 'components/form/FormUtils.jsx';
const scrollIntoView = require('dom-scroll-into-view');

require('./DropDownTree.less');

/**
 *  Komponenta pro výběr z rozbalovacího stromu
 *  @param string className             třída komponenty
 *  $param string filterText            hledaný předvyplněný řezězec
**/
const DropDownTree = class DropDownTree extends AbstractReactComponent {

    static PropTypes = {
        className: React.PropTypes.string,
        opened: React.PropTypes.array,
        label: React.PropTypes.string,
        nullValue: React.PropTypes.object,
        items: React.PropTypes.array.isRequired,
        value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),
        preselect: React.PropTypes.bool,
        onChange: React.PropTypes.func,
        hasFeedback: React.PropTypes.bool,
        help: React.PropTypes.oneOfType([React.PropTypes.object, React.PropTypes.string]),
        addRegistryRecord: React.PropTypes.bool,
    };

    constructor(props) {
        super(props);

        this.bindMethods(
            'getOpenedDefault',             // funkce pro zjisteni uzlu, ktere maji byt automaticky otevrene
            'isNodeOpened',                 // funkce pro kontrolu jestli je uzel otevřený
            'getItemLabel',                 // funkce zjisteni popisku (nazvu) vybrané položky
            'handleItemSelect',             // funkce po kliknutí pro výběr
            'getFirstPossibleRecordType',
            'preselectValue',
            'handleOpenClose',
            'renderNode',
        );

        const opened = [
            ...(props.opened ? props.opened : []),
            ...this.props.items.map(item => this.getOpenedDefault(item))
        ];
        const label = this.getItemLabel(this.props.value, this.props.items);
        this.state = {
            opened,                                             // seznam rozbalenych uzlu
            label: label != '' ? label : this.props.label,              // pokud je vybraná nějaká položka, vypíše se její název, jinak se vypíše defaultní popisek
            value : this.props.value                                    // id vybrane položky
        };
    }

    componentWillReceiveProps(nextProps) {
        if (!(nextProps.preselect && this.preselectValue(nextProps))) {
            const label = this.getItemLabel(nextProps.value, nextProps.items);
            this.setState({
                label: label != '' ? label : nextProps.label,        // pokus je vybrany nejaká položka, vypíše se její název, jinak se vypíše defaultní popisek
                value: nextProps.value                                 // id vybrane položky
            });
            if (nextProps.onChange && this.props.value !== nextProps.value) {
                nextProps.onChange(nextProps.value);
            }

        }

    }

    componentDidMount() {
        const {value} = this.props;
        if (value) {
            this.setState({value});
            if (this.props.onChange) {
                this.props.onChange(this.props.value);
            }
        } else {
            this.preselectValue(this.props);
        }
    }

    preselectValue(nextProps) {
        if (nextProps.value == undefined || nextProps.value == '' || nextProps.value == null) {
            const preselect = this.getFirstPossibleRecordType(nextProps.items);
            if (preselect.found) {
                this.setState({
                    label: this.getItemLabel(preselect.found.id, nextProps.items),
                    value: preselect.found.id
                });

                if (nextProps.onChange) {
                    nextProps.onChange(preselect.found.id, preselect.found);
                }
            }
        }  else {
            return false;
        }
    }


    handleToggleNode(item, e) { 
        e.preventDefault();
        const opened = [...this.state.opened];
        const itemIndex = opened.indexOf(item.id);

        if (itemIndex !== -1) {
            opened.splice(itemIndex, 1);
        } else {
            opened.push(item.id)
        }

        this.setState({opened});
    }

    handleItemSelect(item, e) {
        this.handleOpenClose(e);
        this.setState({
            label: item.name,
            value: item.id,
        });
        if (this.props.onChange) {
            this.props.onChange(item.id, item);
        }
    }

    handleOpenClose(e) {
        const menu = $(e.target).closest('.dropDownTree').find('.menu');
        menu.toggle();

        if(this.props.value) {
            const selectedItem = $(e.target).closest('.dropDownTree').find('.itemID' + this.state.value);
            if(selectedItem.length) {
                scrollIntoView(selectedItem[0], selectedItem[0].parentNode, { onlyScrollIfNeeded: true, alignWithTop:false })
            }
        }

    }

    /**
     * Projde celou hierarchii a najde první typ, pod který lze vložit novou osobu.
     * @param recordTypes typy rejstříků
     * @returns {*}
     */
    getFirstPossibleRecordType(recordTypes) {
        if (!recordTypes || this.props.nullValue) {
            return {
                found: null,
                opened: null
            }
        }

        const opened = [];
        const loop = (type, opened) => {
            if (type.addRecord) {
                return type;
            }

            if (type.children && type.children.length > 0) {
                for (let child of type.children) {
                    const found = loop(child, opened);
                    if (found) {
                        opened.push(child.id);
                        return found;
                    }
                }
            }
            return null;
        };

        let found;
        for (let type of recordTypes) {
            found = loop(type, opened);
            if (found) {
                opened.push(type.id);
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
        const {nullValue, items, onFocus, onBlur, disabled} = this.props;

        const bootInfo = getBootstrapInputComponentInfo(this.props);

        let cls = bootInfo.cls + ' dropDownTree';     // třída komponenty
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        const itemsData = [
            ...(nullValue ? [nullValue] : []),
            ...items
        ];

        const tree = itemsData.map(this.renderNode);

        return (
            <div className={cls}>
                {this.props.label && <label className='control-label'>{this.props.label}</label>}
                <Button
                    className='form-control'
                    onClick={this.handleOpenClose}
                    onFocus={onFocus}
                    onBlur={onBlur}
                    disabled={disabled}
                >
                    <div className='dropDownTree-label'>{this.state.label}</div>
                    <Icon glyph='fa-caret-down' />
                </Button>
                <ul className='menu'>
                    {tree}
                </ul>
                {this.props.hasFeedback && <span className={'glyphicon form-control-feedback glyphicon-' + bootInfo.feedbackIcon}></span>}
                {this.props.help && <span className='help-block'>{this.props.help}</span>}
            </div>
        )
    }

    renderNode(item, depth = 1) {
        const subList = [];
        const isOpened = this.isNodeOpened(item.id);
        if(item.children && item.children.length > 0 && isOpened) {
            for (let i = 0; i < item.children.length; i++) {
                subList[subList.length] = this.renderNode(item.children[i], depth + 1);
            }
        }
        let addClassName = '';
        let clickEvent = this.handleItemSelect.bind(this, item);

        if (this.props.addRegistryRecord && item.addRecord === false) {
            addClassName = ' unavailable';
            clickEvent = null;
        }

        return  <li key={item.id} eventKey={item.id} className={'depth' + depth + ' itemID' + item.id + addClassName}>
            <span className={'switcher ' + (item.children && item.children.length > 0 ? 'enabled' : '') } onClick={this.handleToggleNode.bind(this,item)}>{item.children && item.children.length>0 ? (isOpened ? '−' : '+') : ''}</span>
            <span className={'itemLabel ' + (item.id == this.state.value ? 'active' : '')} onClick={clickEvent}>
                {item.name}
            </span>
            {subList.length > 0 && <ul>{subList}</ul>}
        </li>
    }

    //nastavi vsehny nadrazene uzly vybrane polozce jako otevrene - aby byla vybrana polozka vyiditelna
    getOpenedDefault(item) {
        let opened = [];
        if (item.children && item.children.length > 0) {
            //const childrenLenght = item.children.length;
            opened.concat.apply([],item.children.map(this.getOpenedDefault));
            /*for (let i = 0; i < childrenLenght; i++) {
                opened = opened.concat(item.children[i]);
            }*/
        }
        if(item.id == this.props.value || opened.length > 0) {
            opened[opened.length] = item.id;
        }
        return opened;
    }

    //vrati popisek vybrane polozky
    getItemLabel(value, items) {
        const {nullValue} = this.props;
        if (nullValue && nullValue.id == value) {
            return nullValue.name;
        }
        return this.getItemLabelInt(value, items);
    }

    //vrati popisek vybrane polozky
    getItemLabelInt(value, items) {
        for (let a = 0; a < items.length; a++) {
            const item = items[a];
            if (item.id == value) {
                return item.name;
            }
            if (item.children) {
                const res = this.getItemLabelInt(value, item.children);
                if (res) {
                    return res;
                }
            }
        }
        return null;
    }

    isNodeOpened(nodeID) {
        return this.state.opened.indexOf(nodeID) !== -1;
    }
}

module.exports = DropDownTree;