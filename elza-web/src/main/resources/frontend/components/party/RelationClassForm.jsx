import React from 'react';
import {WebApi} from 'actions/index.jsx';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, FormInput, RegistryField, DatationField} from 'components/index.jsx';
import {Modal, Button, Form, Radio} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'
import {objectById} from 'stores/app/utils.jsx'

import './RelationForm.less'

const USE_UNITDATE_ENUM = {
    NONE: 'NONE',
    ONE: 'ONE',
    INTERVAL: 'INTERVAL',
};

class RelationClassForm extends AbstractReactComponent {

    static PropTypes = {
        relationTypes: React.PropTypes.array.isRequired,
        partyId: React.PropTypes.number
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
        'dateNote',
        'note',
        'source',
        'relationTypeId',
        'relationEntities[].roleType.id',
        'relationEntities[].record',
    ];

    static validate = (values) => {
        const errors = {};
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

        if (values.from.value) {
            let datation, err;
            try {
                datation = DatationField.validate(values.from.value);
            } catch (e) {
                err = e;
            }
            if (!datation) {
                errors.from = {
                    value: err && err.message ? err.message : ' '
                };
            }
        }
        if (values.to.value) {
            let datation, err;
            try {
                datation = DatationField.validate(values.to.value);
            } catch (e) {
                err = e;
            }
            if (!datation) {
                errors.to = {
                    value: err && err.message ? err.message : ' '
                };
            }
        }

        return errors;
    };


    static validateInline = (values) => {
        const errors = {};


        if (values.from.value) {
            let datation, err;
            try {
                datation = DatationField.validate(values.from.value);
            } catch (e) {
                err = e;
            }
            if (!datation) {
                errors.from = {
                    value: err && err.message ? err.message : ' '
                };
            }
        }
        if (values.to.value) {
            let datation, err;
            try {
                datation = DatationField.validate(values.to.value);
            } catch (e) {
                err = e;
            }
            if (!datation) {
                errors.to = {
                    value: err && err.message ? err.message : ' '
                };
            }
        }
        return errors;

    };

    render() {
        const {relationTypes, onClose, handleSubmit, fields: {from, to, relationEntities, dateNote, note, source, relationTypeId}, partyId, submitting} = this.props;

        let relationType = null;
        if (relationTypeId.value !== null) {
            relationType = objectById(relationTypes, relationTypeId.value);
        }

        const roleTypesList = relationType ? relationType.relationRoleTypes.map(i => <option value={i.id} key={i.id}>{i.name}</option>) : null;

        const submit = submitReduxForm.bind(this, RelationClassForm.validate);
        return <Form onSubmit={handleSubmit(submit)}>
            <Modal.Body className="relation-form">
                <div className="flex">
                    <div className="flex-2">
                        {relationTypes.map(i => <Radio inline {...relationTypeId} value={i.id}>{i.name}</Radio>)}
                        {relationType && <div className="relation-entities">
                            <label className="type">{i18n('party.relation.entityInRelation')}</label><Button bsStyle="action" onClick={() => relationEntities.addField({record:null, roleType: {id: null}})}><Icon glyph="fa-plus" /></Button>
                            {/* TODO @compel unikátnost vazby entity */}
                            {relationEntities.map((i,index) => <div className="relation-row" key={index}>
                                <div className="type">
                                    <FormInput componentClass="select" {...i.roleType.id}>
                                        <option key={0}/>
                                        {roleTypesList}
                                    </FormInput>
                                </div>
                                <div className="record">
                                    <RegistryField disabled={!i.roleType.id.value} partyId={partyId} {...i.record} roleTypeId={i.roleType.id.value} />
                                </div>
                                <Button bsStyle="action" onClick={() => relationEntities.removeField(index)}><Icon glyph="fa-times" /></Button>
                            </div>)}
                        </div>}
                    </div>
                    {relationType && relationType.useUnitdate !== USE_UNITDATE_ENUM.NONE && <div className="datation-group flex-1">
                        {(relationType.useUnitdate === USE_UNITDATE_ENUM.INTERVAL || relationType.useUnitdate == USE_UNITDATE_ENUM.ONE) && <div>
                            <DatationField fields={from} label={i18n('party.relation.from')} labelTextual={i18n('party.relation.from.textDate')} labelNote={i18n('party.relation.from.note')} />
                        </div>}
                        {relationType.useUnitdate === USE_UNITDATE_ENUM.INTERVAL && <div>
                            <DatationField fields={to} label={i18n('party.relation.to')} labelTextual={i18n('party.relation.to.textDate')} labelNote={i18n('party.relation.to.note')} />
                        </div>}
                    </div>}
                    <div className="flex-1">
                        <FormInput type="text" label={i18n('party.relation.dateNote')}  {...dateNote} />
                        <FormInput type="text" label={i18n('party.relation.note')} {...note} />
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



