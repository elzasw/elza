/**
 * Atribut - desc item type.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton} from 'components';
import {Tooltip, OverlayTrigger} from 'react-bootstrap';
import {connect} from 'react-redux'
var classNames = require('classnames');
import DescItemString from './DescItemString'
import DescItemUnitid from './DescItemUnitid'
import DescItemText from './DescItemText'
import DescItemInt from './DescItemInt'
import DescItemDecimal from './DescItemDecimal'
import DescItemCoordinates from './DescItemCoordinates'
import DescItemUnitdate from './DescItemUnitdate'
import DescItemPacketRef from './DescItemPacketRef'
import {propsEquals} from 'components/Utils'

require ('./AbstractDescItem.less')

var DescItemType = class DescItemType extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemSpec', 'renderDescItem', 'renderLabel',
                'handleChange', 'handleChangeSpec', 'handleCreatePacket',
                'handleBlur', 'handleFocus', 'handleDescItemTypeLock', 'handleDescItemTypeCopy');
    }

    componentWillReceiveProps(nextProps) {
    }

    shouldComponentUpdate(nextProps, nextState) {
return true;
        var eqProps = ['descItemTypeInfo', 'descItemType', 'rulDataType', 'calendarTypes', 'packetTypes', 'packets', 'locked', 'copy']
        rerturn !propsEquals(this.props, nextProps, eqProps);
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
            <option key={itemSpec.id} value={itemSpec.id}>{itemSpec.name}</option>
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
                key='spec'
                className={cls}
                {...descItemSpecProps}
                value={descItem.descItemSpecId}
                title={descItem.error.spec}
            >
                <option key="novalue"></option>
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
     * Změna hodnoty atributu.
     * @param descItemIndex {Integer} index hodnoty atributu v seznamu
     */
    handleCreatePacket(descItemIndex) {
        this.props.onCreatePacket(descItemIndex);
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
     * @param actions {Array} pole akcí
     * @param locked {Boolean} je atribut uzamčen?
     * @return {Object} view
     */
    renderDescItem(descItemType, descItem, descItemIndex, actions, locked) {
        const {descItemTypeInfo, rulDataType, calendarTypes, packets, packetTypes} = this.props;

        var cls = 'desc-item-type-desc-item-container';
        if (actions.length > 0) {
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
                parts.push(<DescItemPacketRef key="PACKET_REF" {...descItemProps} onCreatePacket={this.handleCreatePacket.bind(this, descItemIndex)} packets={packets} packetTypes={packetTypes} />)
                break;
            case 'UNITDATE':
                parts.push(<DescItemUnitdate key="UNITDATE" {...descItemProps} calendarTypes={calendarTypes} />)
                break;
            case 'UNITID':
                parts.push(<DescItemUnitid key="UNITID" {...descItemProps} />)
                break;
            case 'STRING':
                parts.push(<DescItemString key="STRING" {...descItemProps} />)
                break;
            case 'FORMATTED_TEXT':
            case 'TEXT':
                parts.push(<DescItemText key="TEXT" {...descItemProps} />)
                break;
            case 'DECIMAL':
                parts.push(<DescItemDecimal key="DECIMAL" {...descItemProps} />)
                break;
            case 'INT':
                parts.push(<DescItemInt key="INT" {...descItemProps} />)
                break;
            case 'COORDINATES':
                parts.push(<DescItemCoordinates key="COORDINATES" {...descItemProps} />)
                break;
            case 'ENUM':
                break;
            default:
                parts.push(<div key="unsupported">-unsupported type {rulDataType.code}-</div>)
        }

        var key = descItem.descItemObjectId ? descItem.descItemObjectId + '-' + descItem.position : descItem.position;

        return (
            <div key={descItemType.code + "-" + key} className={cls}>
                <div className='desc-item-value-container'>
                    {parts}
                </div>
                {actions.length > 0 && <div className='desc-item-action-container'>{actions}</div>}
            </div>
        )
    }

    /**
     * Renderování nadpisu atributu - včetně akcí pro atribut.
     * @return {Object} view
     */
    renderLabel() {
        const {copy, locked, descItemType, descItemTypeInfo, conformityInfo} = this.props;

        var actions = [];

        // Sestavení akcí
        actions.push(<NoFocusButton key="copy"onClick={this.handleDescItemTypeCopy}><Icon className={copy ? 'copy' : 'nocopy'} glyph="fa-files-o" /></NoFocusButton>);
        actions.push(<NoFocusButton key="book"><Icon glyph="fa-book" /></NoFocusButton>);
        actions.push(<NoFocusButton key="lock" onClick={this.handleDescItemTypeLock}><Icon className={locked ? 'locked' : 'unlocked'}  glyph="fa-lock" /></NoFocusButton>);

        // Zprávy o chybějících položkách
        var missings = conformityInfo.missings[descItemType.id];
        if (missings) {
            var messages = missings.map(missing => missing.description);
            var tooltip = <Tooltip>{messages}</Tooltip>
            actions.push(<OverlayTrigger key="state" placement="right" overlay={tooltip}>
                <div className='btn btn-default'><Icon glyph="fa-exclamation-triangle" /></div>
            </OverlayTrigger>);
        }

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
            actions.push(<NoFocusButton key="delete" onClick={this.props.onDescItemTypeRemove} title={i18n('subNodeForm.deleteDescItemType')}><Icon glyph="fa-trash" /></NoFocusButton>);
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
        const {onDescItemRemove, onDescItemAdd, descItemType, descItemTypeInfo, locked, conformityInfo} = this.props;

        var label = this.renderLabel();

        var addAction;
        if (descItemTypeInfo.repeatable && !locked) {
            addAction = <div className='desc-item-type-actions'><NoFocusButton onClick={onDescItemAdd} title={i18n('subNodeForm.addDescItem')}><Icon glyph="fa-plus" /></NoFocusButton></div>
        }

        var descItems = descItemType.descItems.map((descItem, descItemIndex) => {
            var actions = new Array;

            if (descItemTypeInfo.repeatable) {
                actions.push(<NoFocusButton onClick={onDescItemRemove.bind(this, descItemIndex)} title={i18n('subNodeForm.deleteDescItem')}><Icon glyph="fa-times" /></NoFocusButton>);
            }

            var errors = conformityInfo.errors[descItem.descItemObjectId];
            if (errors) {
                var messages = errors.map(error => error.description);
                var tooltip = <Tooltip>{messages}</Tooltip>
                actions.push(<OverlayTrigger placement="left" overlay={tooltip}>
                    <div className='btn btn-default'><Icon glyph="fa-exclamation-triangle" /></div>
                </OverlayTrigger>);
            }

            return this.renderDescItem(descItemType, descItem, descItemIndex, actions, locked)
        })

        var cls = classNames({
            'desc-item-type': true,
            active: descItemType.hasFocus,
            ['el-' + descItemTypeInfo.width]: true
        });

        return (
            <div className={cls}>
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
    onCreatePacket: React.PropTypes.func.isRequired,
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
    packets: React.PropTypes.array.isRequired,
    locked: React.PropTypes.bool.isRequired,
    copy: React.PropTypes.bool.isRequired,
    conformityInfo: React.PropTypes.object.isRequired
}

module.exports = connect()(DescItemType);