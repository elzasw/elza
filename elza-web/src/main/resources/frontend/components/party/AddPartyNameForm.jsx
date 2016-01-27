/**
 * Formulář přidání nového jména osobě
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

    return errors;
};

var AddPartyNameForm = class AddPartyNameForm extends AbstractReactComponent {
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
        const {fields: {nameFormTypeId, mainPart, otherPart, degreeBefore, degreeAfter, validFrom, validTo, calendarType}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <Input type="select" label={i18n('party.nameFormType')} {...nameFormTypeId} {...decorateFormField(nameFormTypeId)}>
                            <option value="0" key="0"></option> 
                            {this.props.refTables.partyNameFormTypes.items.map(i=> {return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>})}
                        </Input>
                        <div className="line">
                            <Input type="text" label={i18n('party.degreeBefore')} {...degreeBefore} {...decorateFormField(degreeBefore)} />
                            <Input type="text" label={i18n('party.degreeAfter')} {...degreeAfter} {...decorateFormField(degreeAfter)} />
                        </div>
                        <Input type="text" label={i18n('party.nameMain')} {...mainPart} {...decorateFormField(mainPart)} />
                        <Input type="text" label={i18n('party.nameOther')} {...otherPart} {...decorateFormField(otherPart)} />
                        <Input type="select" label={i18n('party.calendarType')}>
                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.partyTypeId}>{i.name}</option>})}
                        </Input>
                        <div className="line">
                            <Input type="text" label={i18n('party.nameValidFrom')} {...validFrom} {...decorateFormField(validFrom)} />
                            <Input type="text" label={i18n('party.nameValidTo')} {...validTo} {...decorateFormField(validTo)} />
¨                       </div>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'addPartyNameForm',
    fields: ['nameFormTypeId', 'mainPart', 'otherPart', 'degreeBefore', 'degreeAfter', 'validFrom', 'validTo', 'calendarType'],
    validate
},state => ({
    initialValues: state.form.addPartyNameForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyNameForm', data})}
)(AddPartyNameForm)



