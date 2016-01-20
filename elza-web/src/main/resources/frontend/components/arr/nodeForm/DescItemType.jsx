/**
 * Atribut - desc item type.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {i18n, AbstractReactComponent, NoFocusButton} from 'components';
import {Glyphicon} from 'react-bootstrap';
import {connect} from 'react-redux'
var classNames = require('classnames');
import DescItemString from './DescItemString'
import DescItemText from './DescItemText'
import DescItemInt from './DescItemInt'
import DescItemDecimal from './DescItemDecimal'
import DescItemCoordinates from './DescItemCoordinates'
import DescItemUnitdate from './DescItemUnitdate'

require ('./AbstractDescItem.less')

var DescItemType = class DescItemType extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItemSpec', 'renderDescItem', 'renderLabel', 'handleDescItemAdd', 'handleDescItemTypeRemove', 'handleDescItemRemove', 'handleChange', 'handleChangeSpec', 'handleBlur', 'handleFocus');
    }

    componentWillReceiveProps(nextProps) {
    }

    renderDescItemSpec(descItem, descItemIndex) {
        var options = this.props.descItemTypeInfo.descItemSpecs.map(itemSpec => (
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

    handleChange(descItemIndex, value) {
        this.props.onChange(descItemIndex, value);
    }

    handleChangeSpec(descItemIndex, e) {
        this.props.onChangeSpec(descItemIndex, e.target.value);
    }

    handleBlur(descItemIndex) {
        this.props.onBlur(descItemIndex);
    }

    handleFocus(descItemIndex) {
        this.props.onFocus(descItemIndex);
    }

    renderDescItem(descItemType, descItem, descItemIndex, removeAction) {
        var cls = 'desc-item-type-desc-item-container';
        if (removeAction) {
            cls += ' with-action';
        }

        var parts = [];

        if (this.props.descItemTypeInfo.useSpecification) {
            parts.push(
                this.renderDescItemSpec(descItem, descItemIndex)
            );
        }

        var descItemProps = {
            descItem: descItem,
            onChange: this.handleChange.bind(this, descItemIndex),
            onBlur: this.handleBlur.bind(this, descItemIndex),
            onFocus: this.handleFocus.bind(this, descItemIndex),
        }

        parts.push(<div>{this.props.rulDataType.code}-{descItem.id}-{descItemType.type}</div>);
        switch (this.props.rulDataType.code) {
            case 'PARTY_REF':
                break;
            case 'RECORD_REF':
                break;
            case 'PACKET_REF':
                break;
            case 'UNITDATE':
                parts.push(<DescItemUnitdate {...descItemProps} calendarTypes={this.props.calendarTypes} />)
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
                parts.push(<div>-unsupported type {this.props.rulDataType.code}-</div>)
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

    renderLabel() {
        var actions = [];

        // Sestavení akcí
        actions.push(<NoFocusButton><Glyphicon glyph="copy" /></NoFocusButton>);
        actions.push(<NoFocusButton><Glyphicon glyph="book" /></NoFocusButton>);
        actions.push(<NoFocusButton><Glyphicon glyph="lock" /></NoFocusButton>);

        var hasDescItemsForDelete = false;
        if (!this.props.descItemType.hasFocus) {
            this.props.descItemType.descItems.forEach(descItem => {
                if (descItem.touched || typeof descItem.id !== 'undefined') {
                    hasDescItemsForDelete = true;
                }
            });
        }
        if (!hasDescItemsForDelete) {
            if (this.props.descItemTypeInfo.type == 'REQUIRED' || this.props.descItemTypeInfo.type == 'RECOMMENDED') {
            } else {
                hasDescItemsForDelete = true;
            }
        }
        if (hasDescItemsForDelete) {
            actions.push(<NoFocusButton onClick={this.handleDescItemTypeRemove} title={i18n('subNodeForm.deleteDescItemType')}><Glyphicon glyph="remove" /></NoFocusButton>);
        }

        // Render
        return (
            <div className='desc-item-type-label'>
                <div className='title' title={this.props.descItemType.name}>
                    {this.props.descItemTypeInfo.shortcut}
                </div>
                <div className='actions'>
                    {actions}
                </div>
            </div>
        )
    }

    handleDescItemAdd() {
        this.props.onDescItemAdd();
    }

    handleDescItemRemove(descItemIndex) {
        this.props.onDescItemRemove(descItemIndex);
    }

    handleDescItemTypeRemove() {
        this.props.onDescItemTypeRemove();
    }

    render() {
        const {descItemType, descItemTypeInfo} = this.props;

        var label = this.renderLabel();

        var addAction;
        if (descItemTypeInfo.repeatable) {
            addAction = <div className='desc-item-type-actions'><NoFocusButton onClick={this.handleDescItemAdd} title={i18n('subNodeForm.addDescItem')}><Glyphicon glyph="plus" /></NoFocusButton></div>
        }

        var descItems = descItemType.descItems.map((descItem, descItemIndex) => {
            var removeAction;
            if (descItemTypeInfo.repeatable) {
                removeAction = <NoFocusButton onClick={this.handleDescItemRemove.bind(this, descItemIndex)} title={i18n('subNodeForm.deleteDescItem')}><Glyphicon glyph="trash" /></NoFocusButton>
            }
            return this.renderDescItem(descItemType, descItem, descItemIndex, removeAction)
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

module.exports = connect()(DescItemType);

