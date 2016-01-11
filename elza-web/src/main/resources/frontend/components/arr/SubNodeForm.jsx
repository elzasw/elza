/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require ('./SubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {Button, Glyphicon} from 'react-bootstrap';
import {faSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {connect} from 'react-redux'

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

        this.bindMethods('renderAttrValue', 'renderGroup', 'renderAttr');

        var formData = {
            groups: props.formData.groups.map(group => {
                var resultGroup = {
                    ...group
                };

                resultGroup.attrDesc = group.attrDesc.map(attr => {
                    var resultAttr = {
                        ...attr,
                        values: attr.values.map(value => ({value: value.value, prevValue: value.value, specValue: value.specValue}))
                    }

                    return resultAttr;
                });

                return resultGroup;
            })
        }

        this.state = {formData: formData};
    }

    componentWillReceiveProps(nextProps) {
    }

    handleBlur(link, e) {
        var attr = this.state.formData.groups[link.groupIndex].attrDesc[link.attrIndex];
        var value = attr.values[link.valueIndex];

        if (value.error) {
            delete value.error;
            value.value = value.prevValue;

            this.setState({formData: this.state.formData});
        }
    }

    handleChange(link, e) {
        var attr = this.state.formData.groups[link.groupIndex].attrDesc[link.attrIndex];
        var value = attr.values[link.valueIndex];

        var newValue = e.target.value;
        newValue = normalizeText(attr.value, newValue);
        var msg = validateText(newValue, 14);

        if (typeof msg !== 'undefined') {
            value.error = msg;
        } else {
            delete value.error;
        }

        value.value = newValue;

        this.setState({formData: this.state.formData});
    }

    renderAttrValue(attr, link, value, action) {
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

        switch (attr.code) {
            case 'STRING':
                return (
                    <div className={valueContainerCls}>
                        <input
                            className='value'
                            type="text"
                            value={value.value}
                            onChange={this.handleChange.bind(this, link)}
                            onBlur={this.handleBlur.bind(this, link)}
                        />
                        {actionContainer}
                        {value.error && <div>ERR: {value.error}</div>}
                    </div>
                )
            case 'TEXT':
                return (
                    <div className={valueContainerCls}>
                        <textarea 
                            className='value'
                            rows={4}
                            value={value.value}
                            onChange={this.handleChange.bind(this, link)}
                            onBlur={this.handleBlur.bind(this, link)}
                        />
                        {actionContainer}
                        {value.error && <div>ERR: {value.error}</div>}
                    </div>
                )
        }
    }

    renderGroup(group, groupIndex) {
        var attrs = group.attrDesc.map((attr, attrIndex) => (this.renderAttr(attr, attrIndex, groupIndex)));

        return (
            <div className='attr-group'>
                <div className='attr-group-label'>{group.name}</div>
                <div className='attr-group-attrs'>
                    {attrs}
                </div>
            </div>
        )
    }

    renderAttr(attr, attrIndex, groupIndex) {
        var label = (
            <div className='attr-label'>
                {attr.name}
                <Button><Glyphicon glyph="copy" /></Button>
                <Button><Glyphicon glyph="book" /></Button>
                <Button><Glyphicon glyph="lock" /></Button>
            </div>
        )

        var link = {
            groupIndex,
            attrIndex,
        }

        var vals = attr.values.map((value, valueIndex) => {
            var action = attr.multipleValue ? <Button><Glyphicon glyph="remove" /></Button> : undefined
            return this.renderAttrValue(attr, {...link, valueIndex: valueIndex}, value, action);
        })

        var values = (
            <div className='attr-values'>
                {vals}
                {attr.multipleValue && <div className='action'>
                    <Button><Glyphicon glyph="plus" /></Button>
                </div>}
            </div>
        )

        var flexToValue = {'1': '25%', '2': '50%', 3: '75%', '4': '100%'};

        return (
            <div className='attr' style={{width: flexToValue[attr.width]}}>
                {label}
                {values}
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

