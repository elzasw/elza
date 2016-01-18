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
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'

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

var AddPartyPersonForm = class AddPartyPersonForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.props.load(props.initData);

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: {partyTypeId, nameFormTypeId, nameMain, nameOther, degreeBefore, degreeAfter, validRange, calendarType}, handleSubmit, onClose} = this.props;
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
                        <Input type="select" label={i18n('party.calendarType')}>
                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.partyTypeId}>{i.name}</option>})}
                        </Input>
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
    form: 'addPartyPersonForm',
    fields: ['partyTypeId', 'nameFormTypeId', 'nameMain', 'nameOther', 'degreeBefore', 'degreeAfter', 'validRange', 'calendarType'],
    validate
},state => ({
    initialValues: state.form.addPartyPersonForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyPersonForm', data})}
)(AddPartyPersonForm)



