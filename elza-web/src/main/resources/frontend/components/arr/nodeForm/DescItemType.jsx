/**
 * Atribut - desc item type.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton} from 'components';
import {connect} from 'react-redux'
var classNames = require('classnames');
import DescItemString from './DescItemString'
import DescItemText from './DescItemText'
import DescItemInt from './DescItemInt'
import DescItemDecimal from './DescItemDecimal'
import DescItemCoordinates from './DescItemCoordinates'
import DescItemUnitdate from './DescItemUnitdate'
import DescItemPacketRef from './DescItemPacketRef'

require ('./AbstractDescItem.less')

var DescItemType = class DescItemType extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemSpec', 'renderDescItem', 'renderLabel',
                'handleChange', 'handleChangeSpec',
                'handleBlur', 'handleFocus', 'handleDescItemTypeLock', 'handleDescItemTypeCopy');
    }

    componentWillReceiveProps(nextProps) {
    }

    /**
     * Renderování specifikace atributu.
     * @param descItem {Object} objekt hodnoty atributu
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param locked {Boolean}
     * @return {Object} view
     */
    renderDescItemSpec(descItem, descItemIndex, locked) {
        const {descItemTypeInfo} = this.props;

        var options = descItemTypeInfo.descItemSpecs.map(itemSpec => (
            <option value={itemSpec.id}>{itemSpec.name}</option>
        ));

        var cls = classNames({
            'form-control': true,
            value: true,
            'desc-item-spec': true,
            error: descItem.error.spec,
            active: descItem.hasFocus,
        });

        var descItemSpecProps = {
            onChange: this.handleChangeSpec.bind(this, descItemIndex),
            onBlur: this.handleBlur.bind(this, descItemIndex),
            onFocus: this.handleFocus.bind(this, descItemIndex),
            disabled: locked
        }

        return (
            <select
                className={cls}
                {...descItemSpecProps}
                value={descItem.descItemSpecId}
                title={descItem.error.spec}
            >
                <option></option>
                {options}
            </select>
        )
    }

    /**
     * Změna hodnoty atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param value {Object} nová hodnota atibutu
     */
    handleChange(descItemIndex, value) {
        this.props.onChange(descItemIndex, value);
    }

    /**
     * Změna hodnoty specifikace atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param e {Object} event od prvku
     */
    handleChangeSpec(descItemIndex, e) {
        this.props.onChangeSpec(descItemIndex, e.target.value);
    }

    /**
     * Opuštení focusu hodnoty atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleBlur(descItemIndex) {
        this.props.onBlur(descItemIndex);
    }

    /**
     * Získání focusu na hodnotu atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleFocus(descItemIndex) {
        this.props.onFocus(descItemIndex);
    }

    /**
     * Renderování hodnoty atributu.
     * @param descItemType {Object} atribut
     * @param descItem {Object} objekt hodnoty atributu
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     * @param removeAction {Object} pokud je uvedeno, obsahuje view akce pro smazání hodnoty atributu
     * @param locked {Boolean} je atribut uzamčen?
     * @return {Object} view
     */
    renderDescItem(descItemType, descItem, descItemIndex, removeAction, locked) {
        const {descItemTypeInfo, rulDataType, calendarTypes, packets, packetTypes} = this.props;

        var cls = 'desc-item-type-desc-item-container';
        if (removeAction) {
            cls += ' with-action';
        }

        var parts = [];

        if (descItemTypeInfo.useSpecification) {
            parts.push(
                this.renderDescItemSpec(descItem, descItemIndex, locked)
            );
        }

        var descItemProps = {
            descItem: descItem,
            onChange: this.handleChange.bind(this, descItemIndex),
            onBlur: this.handleBlur.bind(this, descItemIndex),
            onFocus: this.handleFocus.bind(this, descItemIndex),
            locked: locked
        }

        //parts.push(<div>{rulDataType.code}-{descItem.id}-{descItemType.type}</div>);
        switch (rulDataType.code) {
            case 'PARTY_REF':
                break;
            case 'RECORD_REF':
                break;
            case 'PACKET_REF':
                parts.push(<DescItemPacketRef {...descItemProps} packets={packets} packetTypes={packetTypes} />)
                break;
            case 'UNITDATE':
                parts.push(<DescItemUnitdate {...descItemProps} calendarTypes={calendarTypes} />)
                break;
            case 'UNITID':
                break;
            case 'STRING':
                parts.push(<DescItemString {...descItemProps} />)
                break;
            case 'FORMATTED_TEXT':
            case 'TEXT':
                parts.push(<DescItemText {...descItemProps} />)
                break;
            case 'DECIMAL':
                parts.push(<DescItemDecimal {...descItemProps} />)
                break;
            case 'INT':
                parts.push(<DescItemInt {...descItemProps} />)
                break;
            case 'COORDINATES':
                parts.push(<DescItemCoordinates {...descItemProps} />)
                break;
            case 'ENUM':
                break;
            default:
                parts.push(<div>-unsupported type {rulDataType.code}-</div>)
        }

        return (
            <div className={cls}>
                <div className='desc-item-value-container'>
                    {parts}
                </div>
                {removeAction && <div className='desc-item-action-container'>{removeAction}</div>}
            </div>
        )
    }

    /**
     * Renderování nadpisu atributu - včetně akcí pro atribut.
     * @return {Object} view
     */
    renderLabel() {
        const {copy, locked, descItemType, descItemTypeInfo} = this.props;

        var actions = [];

        // Sestavení akcí
        actions.push(<NoFocusButton onClick={this.handleDescItemTypeCopy}><Icon className={copy ? 'copy' : 'nocopy'} glyph="fa-files-o" /></NoFocusButton>);
        actions.push(<NoFocusButton><Icon glyph="fa-book" /></NoFocusButton>);
        actions.push(<NoFocusButton onClick={this.handleDescItemTypeLock}><Icon className={locked ? 'locked' : 'unlocked'}  glyph="fa-lock" /></NoFocusButton>);

        var hasDescItemsForDelete = false;
        if (!descItemType.hasFocus) {
            descItemType.descItems.forEach(descItem => {
                if (descItem.touched || typeof descItem.id !== 'undefined') {
                    hasDescItemsForDelete = true;
                }
            });
        }
        if (!hasDescItemsForDelete) {
            if (descItemTypeInfo.type == 'REQUIRED' || descItemTypeInfo.type == 'RECOMMENDED') {
            } else {
                hasDescItemsForDelete = true;
            }
        }
        if (hasDescItemsForDelete) {
            actions.push(<NoFocusButton onClick={this.props.onDescItemTypeRemove} title={i18n('subNodeForm.deleteDescItemType')}><Icon glyph="fa-trash" /></NoFocusButton>);
        }

        // Render
        return (
            <div className='desc-item-type-label'>
                <div className='title' title={descItemType.name}>
                    {descItemTypeInfo.shortcut}
                </div>
                <div className='actions'>
                    {actions}
                </div>
            </div>
        )
    }

    /**
     * Zapnutí/vypnutí zámku na atributu.
     */
    handleDescItemTypeLock() {
        this.props.onDescItemTypeLock(!this.props.locked);
    }

    /**
     * Zapnutí/vypnutí opakovaného kopírování na atributu.
     */
    handleDescItemTypeCopy() {
        this.props.onDescItemTypeCopy(!this.props.copy);
    }

    render() {
        const {onDescItemRemove, onDescItemAdd, descItemType, descItemTypeInfo, locked} = this.props;

        var label = this.renderLabel();

        var addAction;
        if (descItemTypeInfo.repeatable && !locked) {
            addAction = <div className='desc-item-type-actions'><NoFocusButton onClick={onDescItemAdd} title={i18n('subNodeForm.addDescItem')}><Icon glyph="fa-plus" /></NoFocusButton></div>
        }

        var descItems = descItemType.descItems.map((descItem, descItemIndex) => {
            var removeAction;
            if (descItemTypeInfo.repeatable) {
                removeAction = <NoFocusButton onClick={onDescItemRemove.bind(this, descItemIndex)} title={i18n('subNodeForm.deleteDescItem')}><Icon glyph="fa-trash" /></NoFocusButton>
            }
            return this.renderDescItem(descItemType, descItem, descItemIndex, removeAction, locked)
        })

        var cls = classNames({
            'desc-item-type': true,
            active: descItemType.hasFocus
        });

        var flexToValue = {'1': '25%', '2': '50%', 3: '75%', '4': '100%'};
        var width = flexToValue[descItemType.width];
        width = width || '50%';

        return (
            <div className={cls} style={{width: width}}>
                {label}
                <div className='desc-item-type-desc-items'>
                    {descItems}
                </div>
                {addAction}
            </div>
        )
    }
}

DescItemType.propTypes = {
    onChange: React.PropTypes.func.isRequired,
    onChangeSpec: React.PropTypes.func.isRequired,
    onBlur: React.PropTypes.func.isRequired,
    onFocus: React.PropTypes.func.isRequired,
    onDescItemTypeRemove: React.PropTypes.func.isRequired,
    onDescItemTypeLock: React.PropTypes.func.isRequired,
    onDescItemTypeCopy: React.PropTypes.func.isRequired,
    onDescItemRemove: React.PropTypes.func.isRequired,
    onDescItemAdd: React.PropTypes.func.isRequired,
    descItemTypeInfo: React.PropTypes.object.isRequired,
    descItemType: React.PropTypes.object.isRequired,
    rulDataType: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.object.isRequired,
    locked: React.PropTypes.bool.isRequired,
    copy: React.PropTypes.bool.isRequired,
}

module.exports = connect()(DescItemType);