/**
 * Formulář přidání nové Osoby
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'

const validate = (values, props) => {
    const errors = {};

    if (props.create && !values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    return errors;
};

var AddPartyForm = class AddPartyForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.props.load(props.initData);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: {nameMain, nameOther, degreeBefore, degreeAfter, nameFormType, validFrom, validTo}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="select" label={i18n('party.nameFormType')} {...nameFormType} {...decorateFormField(nameFormType)}>
                            <option>Jedna</option>
                            <option>Dva</option>
                            <option>3ři</option> 
                        </Input>
                        <Input type="text" label={i18n('party.degreeBefore')} {...degreeBefore} {...decorateFormField(degreeBefore)} />
                        <Input type="text" label={i18n('party.nameMain')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="text" label={i18n('party.nameOther')} {...nameOther} {...decorateFormField(nameOther)} />
                        <Input type="text" label={i18n('party.degreeAfter')} {...degreeAfter} {...decorateFormField(degreeAfter)} />
                        <Input type="text" label={i18n('party.nameValidFrom')} {...validFrom} {...decorateFormField(validFrom)} />
                        <Input type="text" label={i18n('party.nameValidTo')} {...validTo} {...decorateFormField(validTo)} />
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.create')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addPartyForm',
    fields: ['nameMain', 'nameOther', 'degreeBefore', 'degreeAfter', 'nameFormType', 'validFrom', 'validTo'],
    validate
},state => ({
    initialValues: state.form.addPartyForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyForm', data})}
)(AddPartyForm)



