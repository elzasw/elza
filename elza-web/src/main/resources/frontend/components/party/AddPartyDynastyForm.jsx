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
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'


const validate = (values, props) => {
    const errors = {};

    if (!values.nameMain) {
        errors.nameMain = i18n('global.validation.required');
    }

    if ((!values.nameFormTypeId || values.nameFormTypeId==0 )) {
        errors.nameFormTypeId = i18n('global.validation.required');
    }

    if ((!values.recordTypeId || values.recordTypeId==0 )) {
        errors.recordTypeId = i18n('global.validation.required');
    }

    if ((!values.calendarTypeIdFrom || values.calendarTypeIdFrom==0 )) {
        errors.calendarTypeIdFrom = i18n('global.validation.required');
    }

    if ((!values.calendarTypeIdTo || values.calendarTypeIdTo==0 )) {
        errors.calendarTypeIdTo = i18n('global.validation.required');
    }

    return errors;
};

var AddPartyDynastyForm = class AddPartyDynastyForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.props.load(props.initData);
        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        var recordTypes = [];
        for(var i=0; i<this.props.refTables.partyTypes.items.length; i++){
            if(this.props.refTables.partyTypes.items[i].partyTypeId == this.props.initData.partyTypeId){
                recordTypes = this.props.refTables.partyTypes.items[i].registerTypes;
            }
        }
        const {fields: {partyTypeId, nameFormTypeId, recordTypeId, nameMain, nameOther, validFrom, validTo, calendarTypeIdFrom, calendarTypeIdTo}, handleSubmit, onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <div className="line">
                            <Input type="select" label={i18n('party.recordType')} {...recordTypeId} {...decorateFormField(recordTypeId)}>
                                <option value="0" key="0"></option> 
                                {recordTypes.map(i=> {return <option value={i.id} key={i.nameFormTypeId}>{i.name}</option>})}
                            </Input>
                            <Input type="select" label={i18n('party.nameFormType')} {...nameFormTypeId} {...decorateFormField(nameFormTypeId)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.partyNameFormTypes.items.map(i=> {return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>})}
                            </Input>
                        </div>
                        <hr/>
                        <Input type="text" label={i18n('party.nameMain')} {...nameMain} {...decorateFormField(nameMain)} />
                        <Input type="text" label={i18n('party.nameOther')} {...nameOther} {...decorateFormField(nameOther)} />
                        <hr/>
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
    form: 'addPartyDynastyForm',
    fields: ['partyTypeId', 'nameFormTypeId', 'recordTypeId', 'nameMain', 'nameOther', 'validFrom', 'validTo', 'calendarTypeIdFrom', 'calendarTypeIdTo'],
    validate
},state => ({
    initialValues: state.form.addPartyDynastyForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyDynastyForm', data})}
)(AddPartyDynastyForm)



