import React from 'react';
import {WebApi} from 'actions/index.jsx';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, FormInput, RegistryField, DatationField} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'

import './RelationForm.less'

const USE_UNITDATE_ENUM = {
    NONE: 'NONE',
    ONE: 'ONE',
    INTERVAL: 'INTERVAL',
};

class RelationForm extends AbstractReactComponent {

    static PropTypes = {
        relationType: React.PropTypes.object.isRequired,
        partyId: React.PropTypes.number
    };

    static fields = [
        'from.calendarTypeId',
        'from.valueFrom',
        'from.textDate',
        'from.note',
        'to.calendarTypeId',
        'to.valueFrom',
        'to.textDate',
        'to.note',
        'dateNote',
        'note',
        'source',
        'relationEntities[].roleType.id',
        'relationEntities[].record',
    ];

    static validate = (values) => {
        const errors = {};


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

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    render() {
        const {relationType, refTables: {calendarTypes}, onClose, handleSubmit, fields: {from, to, relationEntities, dateNote, note, source}, partyId} = this.props;
        const {relationRoleTypes} = relationType;
        const calendars = calendarTypes ? calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>}) : null;
        const roleTypesList = relationRoleTypes ? relationRoleTypes.map(i=> {return <option value={i.id} key={i.id}>{i.name}</option>}) : null;

        const submit = submitReduxForm.bind(this, RelationForm.validate);
        return <Form onSubmit={handleSubmit(submit)}>
            <Modal.Body className="relation-form">
                <div className="block entity relations">
                    <div className="relation-entities">
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
                    </div>
                </div>
                {!relationType.useUnitdate !== USE_UNITDATE_ENUM.NONE && <div className="datations">
                    {(relationType.useUnitdate === USE_UNITDATE_ENUM.INTERVAL || relationType.useUnitdate == USE_UNITDATE_ENUM.ONE) && <div>
                        <DatationField fields={from} label={i18n('party.relation.from')} labelTextual={i18n('party.relation.from.textDate')} labelNote={i18n('party.relation.from.note')} />
                    </div>}
                    {relationType.useUnitdate === USE_UNITDATE_ENUM.INTERVAL && <div>
                        <DatationField fields={to} label={i18n('party.relation.to')} labelTextual={i18n('party.relation.to.textDate')} labelNote={i18n('party.relation.to.note')} />
                    </div>}
                </div>}
                <div className="footer">
                    <FormInput type="text" label={i18n('party.relation.dateNote')}  {...dateNote} />
                    <FormInput type="text" label={i18n('party.relation.note')} {...note} />
                    <FormInput componentClass="textarea" label={i18n('party.relation.sources')} {...source} />
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button type="submit">{i18n('global.action.store')}</Button>
                <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
            </Modal.Footer>
        </Form>;
    }
}

export default reduxForm({
    form: 'relationForm',
    fields: RelationForm.fields,
},state => ({
    refTables: state.refTables
})
)(RelationForm)
