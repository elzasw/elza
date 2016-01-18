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

    if (!values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if ((!values.nameFormTypeId || values.nameFormTypeId==0 )) {
        errors.nameFormTypeId = i18n('global.validation.required');
    }

    return errors;
};

var AddPartyOtherForm = class AddPartyOtherForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());

        this.props.load(props.initData);

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: {partyTypeId, nameFormTypeId, nameMain, nameOther, degreeBefore, degreeAfter, validRange}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="select" label={i18n('party.nameFormType')} {...nameFormTypeId} {...decorateFormField(nameFormTypeId)}>
                            <option value="0" key="0"></option> 
                            {this.props.refTables.partyNameFormTypes.items.map(i=> {return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('party.degreeBefore')} {...degreeBefore} {...decorateFormField(degreeBefore)} />
                        <Input type="text" label={i18n('party.nameMain')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="text" label={i18n('party.nameOther')} {...nameOther} {...decorateFormField(nameOther)} />
                        <Input type="text" label={i18n('party.degreeAfter')} {...degreeAfter} {...decorateFormField(degreeAfter)} />
                        <Input type="text" label={i18n('party.nameValidRange')} {...validRange} {...decorateFormField(validRange)} />
                        <Input type="hidden" {...partyTypeId} {...decorateFormField(partyTypeId)} />
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
    form: 'addPartyOtherForm',
    fields: ['partyTypeId', 'nameFormTypeId', 'nameMain', 'nameOther', 'degreeBefore', 'degreeAfter', 'validRange'],
    validate
},state => ({
    initialValues: state.form.addPartyOtherForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyOtherForm', data})}
)(AddPartyOtherForm)



