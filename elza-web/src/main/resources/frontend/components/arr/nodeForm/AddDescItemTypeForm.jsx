/**
 * Formulář přidání nové desc item type.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {Autocomplete, AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

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

    render() {
        const {fields: {descItemTypeId}, handleSubmit, onClose} = this.props;

        var ac = (
            <Autocomplete
            label={i18n('subNodeForm.descItemType')}
            {...descItemTypeId}
            {...decorateFormField(descItemTypeId)}
            items={this.props.descItemTypes}
            />
        )

        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        {true && ac}
                        {false && <Input type="select" label={i18n('subNodeForm.descItemType')} {...descItemTypeId} {...decorateFormField(descItemTypeId)}>
                            <option></option>
                            {this.props.descItemTypes.map(i=> <option value={i.id}>{i.name}</option>)}
                        </Input>}
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.add')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addDescItemTypeForm',
    fields: ['descItemTypeId'],
    validate
},state => ({}),
{}
)(AddDescItemTypeForm)



