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
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes'

const validate = (values, props) => {
    const errors = {};

    if (props.create && !values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if (props.create && (!values.nameFormTypeId || values.nameFormTypeId==0 )) {
        errors.nameFormTypeId = i18n('global.validation.required');
    }

    return errors;
};

var AddPartyForm = class AddPartyForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());

        this.props.load(props.initData);

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: {nameMain, nameOther, degreeBefore, degreeAfter, nameFormTypeId, validRange}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="select" label={i18n('party.nameFormType')} {...nameFormTypeId} {...decorateFormField(nameFormTypeId)}>
                            <option value="0"></option> 
                            {this.props.refTables.partyNameFormTypes.items.map(i=> {return <option value={i.nameFormTypeId}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('party.degreeBefore')} {...degreeBefore} {...decorateFormField(degreeBefore)} />
                        <Input type="text" label={i18n('party.nameMain')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="text" label={i18n('party.nameOther')} {...nameOther} {...decorateFormField(nameOther)} />
                        <Input type="text" label={i18n('party.degreeAfter')} {...degreeAfter} {...decorateFormField(degreeAfter)} />
                        <Input type="text" label={i18n('party.nameValidRange')} {...validRange} {...decorateFormField(validRange)} />
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
    fields: ['nameMain', 'nameOther', 'degreeBefore', 'degreeAfter', 'nameFormTypeId', 'validRange'],
    validate
},state => ({
    initialValues: state.form.addPartyForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyForm', data})}
)(AddPartyForm)



