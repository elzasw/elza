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

    if (!values.mainPart) {
        errors.mainPart = i18n('global.validation.required');
    }

    if ((!values.calendarTypeIdFrom || values.calendarTypeIdFrom==0 )) {
        errors.calendarTypeIdFrom = i18n('global.validation.required');
    }

    if ((!values.calendarTypeIdTo || values.calendarTypeIdTo==0 )) {
        errors.calendarTypeIdTo = i18n('global.validation.required');
    }

    if ((!values.nameFormTypeId || values.nameFormTypeId==0 )) {
        errors.nameFormTypeId = i18n('global.validation.required');
    }

    return errors;
};

var PartyNameForm = class PartyNameForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        //import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
        this.props.load(props.initData);

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fields: {partyNameId, nameFormTypeId, mainPart, otherPart, degreeBefore, degreeAfter, validFrom, validTo, calendarTypeIdFrom, calendarTypeIdTo}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <input type="hidden" {...partyNameId}/>
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
                        <div className="line">
                            <Input type="text" label={i18n('party.nameValidFrom')} {...validFrom} {...decorateFormField(validFrom)} />
                            <Input type="select" label={i18n('party.calendarTypeFrom')} {...calendarTypeIdFrom} {...decorateFormField(calendarTypeIdFrom)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id}>{i.name}</option>})}
                            </Input>
                        </div>
                        <div className="line">
                            <Input type="text" label={i18n('party.nameValidTo')} {...validTo} {...decorateFormField(validTo)} />
                            <Input type="select" label={i18n('party.calendarTypeTo')} {...calendarTypeIdTo} {...decorateFormField(calendarTypeIdTo)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id}>{i.name}</option>})}
                            </Input>
                        </div>
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
    form: 'PartyNameForm',
    fields: ['partyNameId','nameFormTypeId', 'mainPart', 'otherPart', 'degreeBefore', 'degreeAfter', 'validFrom', 'validTo', 'calendarTypeIdFrom', 'calendarTypeIdTo'],
    validate
},state => ({
    initialValues: state.form.partyNameForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'partyNameForm', data})}
)(PartyNameForm)



