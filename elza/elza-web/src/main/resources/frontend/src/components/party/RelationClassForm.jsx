import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, FormInput} from 'components/shared';
import {Modal, Button, Form, Radio, FormGroup, HelpBlock} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'
import {submitForm} from 'components/form/FormUtils.jsx'
import {objectById} from 'stores/app/utils.jsx'

import './RelationForm.less'
import DatationField from "./DatationField";
import RegistryField from "../registry/RegistryField";

const USE_UNITDATE_ENUM = { // TODO @compel move to party.jsx
    NONE: 'NONE',
    ONE: 'ONE',
    INTERVAL: 'INTERVAL',
};

class RelationClassForm extends AbstractReactComponent {

    static PropTypes = {
        relationTypes: PropTypes.array.isRequired,
        apTypes: PropTypes.object.isRequired,
        partyId: PropTypes.number
    };

    static fields = [
        'from.calendarTypeId',
        'from.value',
        'from.textDate',
        'from.note',
        'to.calendarTypeId',
        'to.value',
        'to.textDate',
        'to.note',
        'note',
        'source',
        'relationTypeId',
        'relationEntities[].roleType.id',
        'relationEntities[].record',
    ];

    static validate = (values) => {
        const errors = RelationClassForm.validateInline(values);
        if (!values.relationTypeId) {
            errors.relationTypeId = i18n('global.validation.required');
        }

        for (let i=0; i<values.relationEntities.length; i++) {
            if (!values.relationEntities[i].record) {
                errors['relationEntities['+i+'].record'] = i18n('party.relation.errors.undefinedRecord');
            }
        }

        for (let i=0; i<values.relationEntities.length; i++) {
            if (!values.relationEntities[i].roleType.id) {
                errors['relationEntities['+i+'].roleType.id'] = i18n('party.relation.errors.undefinedRoleType');
            }
        }

        return errors;
    };


    static validateInline = (values) => {
        const errors = {};


        errors.from = DatationField.reduxValidate(values.from);
        errors.to = DatationField.reduxValidate(values.to);

        if (!errors.from) {
            delete errors.from
        }
        if (!errors.to) {
            delete errors.to
        }

        if (values.note && values.note.length > 1000) {
            errors.note = i18n('party.relation.errors.invalidNoteLength', 1000)
        }

        return errors;

    };

    submitReduxForm = (values, dispatch) => submitForm(RelationClassForm.validate,values,this.props,this.props.onSubmitForm,dispatch);

    render() {
        const {relationTypes, onClose, handleSubmit, fields: {from, to, relationEntities, note, source, relationTypeId}, partyId, submitting, apTypesMap} = this.props;

        let relationType = null;
        if (relationTypeId.value !== null) {
            relationType = objectById(relationTypes, relationTypeId.value);
        }
        const roleTypesList = relationType ? relationType.relationRoleTypes : null;
        const usedRoles = relationEntities.map(i => parseInt(i.roleType.id.value));
        return <Form onSubmit={handleSubmit(this.submitReduxForm)}>
            <Modal.Body className="dialog-3-col relation-form">
                <div className="flex">
                    <div className="flex-2 col">
                        <FormGroup validationState={relationTypeId.touched && relationTypeId.invalid ? 'error' : null}>
                            <div className="relation-class-select">{relationTypes.map(i => <Radio inline checked {...relationTypeId} value={i.id} onBlur={()=>{/* musí být kvůli chybné implementaci v REDUX */}}>{i.name}</Radio>)}</div>
                            {relationTypeId.touched && relationTypeId.invalid && <HelpBlock>{relationTypeId.error}</HelpBlock>}
                        </FormGroup>
                        {relationType && <div className="relation-entities">
                            <label className="type">{i18n('party.relation.entityInRelation')}</label><Button bsStyle="action" onClick={() => relationEntities.addField({record:null, roleType: {id: null}})}><Icon glyph="fa-plus" /></Button>
                            {relationEntities.map((i,index) => <div className="relation-row" key={index}>
                                <div className="type">
                                    <FormInput componentClass="select" {...i.roleType.id}>
                                        <option key={0}/>
                                        {roleTypesList && roleTypesList.filter(t => t.id == i.roleType.id.value || t.repeatable || usedRoles.indexOf(t.id) === -1).map(v => {
                                            let disabled = false;

                                            if (i.record != null && i.record.value != null && i.record.value.apTypeId != null) {
                                                let apTypeId = i.record.value.apTypeId;
                                                if (apTypesMap[apTypeId] == null || apTypesMap[apTypeId].indexOf(v.id) === -1) {
                                                    disabled = true;
                                                }
                                            }

                                            return <option disabled={(disabled) ? "disabled" : ""} value={v.id} key={v.id}>{v.name}</option>})}
                                    </FormInput>
                                </div>
                                <div className="record">
                                    <RegistryField disabled={!i.roleType.id.value} partyId={partyId} {...i.record} roleTypeId={i.roleType.id.value} footer={true} footerButtons={false} />
                                </div>
                                <Button bsStyle="action" onClick={() => relationEntities.removeField(index)}><Icon glyph="fa-times" /></Button>
                            </div>)}
                        </div>}
                    </div>
                    {relationType && relationType.useUnitdate !== USE_UNITDATE_ENUM.NONE && <div className="datation-group flex-1 col">
                        {(relationType.useUnitdate == USE_UNITDATE_ENUM.ONE) && <div>
                            <DatationField fields={from} label={i18n('party.relation.date')} labelTextual={i18n('party.relation.date.textDate')} labelNote={i18n('party.relation.date.note')} />
                        </div>}
                        {(relationType.useUnitdate === USE_UNITDATE_ENUM.INTERVAL) && <div>
                            <DatationField fields={from} label={i18n('party.relation.from')} labelTextual={i18n('party.relation.from.textDate')} labelNote={i18n('party.relation.from.note')} />
                        </div>}
                        {relationType.useUnitdate === USE_UNITDATE_ENUM.INTERVAL && <div>
                            <DatationField fields={to} label={i18n('party.relation.to')} labelTextual={i18n('party.relation.to.textDate')} labelNote={i18n('party.relation.to.note')} />
                        </div>}
                    </div>}
                    <div className="flex-1 col">
                        <FormInput componentClass="textarea" label={i18n('party.relation.note')} {...note} />
                        <FormInput componentClass="textarea" label={i18n('party.relation.sources')} {...source} />
                    </div>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit" disabled={submitting}>{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose} disabled={submitting}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'relationClassForm',
    fields: RelationClassForm.fields,
    validate: RelationClassForm.validateInline
})(RelationClassForm)
