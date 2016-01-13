/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require ('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {Button, Glyphicon} from 'react-bootstrap';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import DescItemString from './nodeForm/DescItemString'

function validateText(value, maxLength) {
    if (value && value.length >= maxLength) {
        return "Moc dlouhy";
    }
}
function normalizeText(prevValue, value) {
    return value;
}

var SubNodeForm = class SubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderDescItem', 'renderGroup', 'renderDescItemType');
console.log("FFFF", props);
        var formData = {
            groups: props.formData.map(group => {
                var resultGroup = {
                    ...group
                };

                resultGroup.descItemTypes = group.descItemTypes.map(descItemType => {

                    var resultDescItemType = {
                        ...descItemType,
                        descItems: descItemType.descItems.map(descItem => ({...descItem, prevDescItemSpecId: descItem.descItemSpecId, prevValue: descItem.value}))
                    }

                    return resultDescItemType;
                });

                return resultGroup;
            })
        }

        this.state = {formData: formData};
    }

    componentWillReceiveProps(nextProps) {
    }

    handleBlur(link, e) {
        var descItemType = this.state.formData.groups[link.groupIndex].descItemTypes[link.descItemTypeIndex];
        var descItem = descItemType.descItems[link.descItemIndex];

        if (descItem.error) {
            delete descItem.error;
            descItem.value = descItem.prevValue;

            this.setState({formData: this.state.formData});
        }
    }

    handleChange(link, e) {
        var descItemType = this.state.formData.groups[link.groupIndex].descItemTypes[link.descItemTypeIndex];
        var descItem = descItemType.descItems[link.descItemIndex];

        var newValue = e.target.value;
        newValue = normalizeText(descItem.value, newValue);
        var msg = validateText(newValue, 14);

        if (typeof msg !== 'undefined') {
            descItem.error = msg;
        } else {
            delete descItem.error;
        }

        descItem.value = newValue;

        this.setState({formData: this.state.formData});
    }

    renderDescItem(descItemType, link, descItem, action) {
        var actionContainer;
        if (action) {
            actionContainer = (
                <div className='action'>
                    {action}
                </div>
            )
        }

        var valueContainerCls = 'value-container';
        if (action) {
            valueContainerCls += ' action';
        }

        var rulDataType = this.props.rulDataTypes.items[indexById(this.props.rulDataTypes.items, descItemType.dataTypeId)];

        switch (rulDataType.code) {
            case 'STRING':
                return (
                    <DescItemString descItem={descItem}/>
                )
                return (
                    <div className={valueContainerCls}>
                        <input
                            className='form-control value'
                            type="text"
                            value={descItem.value}
                            onChange={this.handleChange.bind(this, link)}
                            onBlur={this.handleBlur.bind(this, link)}
                        />
                        {actionContainer}
                        {descItem.error && <div>ERR: {descItem.error}</div>}
                    </div>
                )
            case 'TEXT':
                return (
                    <div className={valueContainerCls}>
                        <textarea 
                            className='form-control value'
                            rows={4}
                            value={descItem.value}
                            onChange={this.handleChange.bind(this, link)}
                            onBlur={this.handleBlur.bind(this, link)}
                        />
                        {actionContainer}
                        {descItem.error && <div>ERR: {descItem.error}</div>}
                    </div>
                )
        }
    }

    renderGroup(group, groupIndex) {
        var descItemTypes = group.descItemTypes.map((descItemType, descItemTypeIndex) => (this.renderDescItemType(descItemType, descItemTypeIndex, groupIndex)));

        return (
            <div className='attr-group'>
                {false && <div className='attr-group-label'>{group.name}</div>}
                <div className='attr-group-attrs'>
                    {descItemTypes}
                </div>
            </div>
        )
    }

    renderDescItemType(descItemType, descItemTypeIndex, groupIndex) {
        var label = (
            <div className='attr-label'>
                {descItemType.name}
                <Button><Glyphicon glyph="copy" /></Button>
                <Button><Glyphicon glyph="book" /></Button>
                <Button><Glyphicon glyph="lock" /></Button>
            </div>
        )

        var link = {
            groupIndex,
            descItemTypeIndex,
        }

        var vals = descItemType.descItems.map((descItem, descItemIndex) => {
            var action = descItemType.multipleValue ? <Button><Glyphicon glyph="remove" /></Button> : undefined
            return this.renderDescItem(descItemType, {...link, descItemIndex: descItemIndex}, descItem, action);
        })

        var descItems = (
            <div className='attr-values'>
                {vals}
                {descItemType.multipleValue && <div className='action'>
                    <Button><Glyphicon glyph="plus" /></Button>
                </div>}
            </div>
        )

        var flexToValue = {'1': '25%', '2': '50%', 3: '75%', '4': '100%'};

        return (
            <div className='attr' style={{width: flexToValue[descItemType.width]}}>
                {label}
                {descItems}
            </div>
        )
    }

    render() {
        var actions = (
            <div className='actions'>
                <Button><Glyphicon glyph="plus" />Přidat prvek</Button>
                <Button><Glyphicon glyph="lock" />Odemknout vše</Button>
                <Button><Glyphicon glyph="plus" />Přidat JP před</Button>
                <Button><Glyphicon glyph="plus" />Přidat JP za</Button>
                <Button><Glyphicon glyph="list" />Rejstříky</Button>
                <Button><Glyphicon glyph="remove" />Zrušit JP</Button>
            </div>
        )

        var groups = this.state.formData.groups.map((group, groupIndex) => (this.renderGroup(group, groupIndex)));

        return (
            <div className='node-form'>
                {actions}
                {groups}
            </div>
        )
    }
}

module.exports = connect()(SubNodeForm);

