/**
 * Formulář přidání nové desc item type.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n, Icon} from 'components/index.jsx';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'

require ('./AddDescItemTypeForm.less')

const validate = (values, props) => {
    const errors = {};

    if (!values.descItemTypeId) {
        errors.descItemTypeId = i18n('global.validation.required');
    }

    return errors;
};

var AddDescItemTypeForm = class AddDescItemTypeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        //this.bindMethods('');

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    renderItem(item, isHighlighted, isSelected) {
        var cls = 'item';

        cls += ' type-' + item.type.toLowerCase();

        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (
                <div className={cls} key={item.id} >
                    <div className="name" title={item.name}>{item.name}</div>
                </div>
        )
    }

    render() {
        const {fields: {descItemTypeId}, handleSubmit, onClose} = this.props;

        var submitForm = submitReduxForm.bind(this, validate)

        var descItemTypeValue;
        if (typeof descItemTypeId.value !== 'undefined') {
            var index = indexById(this.props.descItemTypes, descItemTypeId.value);
            if (index !== null) {
                descItemTypeValue = this.props.descItemTypes[index]
            }
        }

        var ac = (
            <div className="autocomplete-desc-item-type">
                <Autocomplete
                    label={i18n('subNodeForm.descItemType')}
                {...descItemTypeId}
                {...decorateFormField(descItemTypeId)}
                    value={descItemTypeValue}
                    items={this.props.descItemTypes}
                    renderItem={this.renderItem}
                    onBlurValidation={false}
                />
            </div>
        )

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit(submitForm)}>
                        <div>
                            {this.props.descItemTypes.map(i=> {
                                if (i.type == "POSSIBLE") {
                                    return <a className="add-link btn btn-link" key={i.id} onClick={() => {
                                        this.props.onSubmit2({descItemTypeId:i.id});
                                   }}><Icon glyph="fa-plus" />{i.name}</a>
                                }
                            })}
                        </div>
                        {true && ac}
                        {false && <Input type="select" label={i18n('subNodeForm.descItemType')} {...descItemTypeId} {...decorateFormField(descItemTypeId)}>
                            <option></option>
                            {this.props.descItemTypes.map(i=> <option value={i.id}>{i.name}</option>)}
                        </Input>}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit(submitForm)}>{i18n('global.action.add')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addDescItemTypeForm',
    fields: ['descItemTypeId'],
},state => ({}),
{}
)(AddDescItemTypeForm)



