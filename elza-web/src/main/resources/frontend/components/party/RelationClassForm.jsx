import React from 'react';
import {WebApi} from 'actions/index.jsx';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, FormInput, RegistryField} from 'components/index.jsx';
import {Modal, Button, Form, Radio} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'
import {objectById} from 'stores/app/utils.jsx'

import './PartyFormStyles.less';

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
        'from.textDate',
        'to.calendarTypeId',
        'to.textDate',
        'dateNote',
        'note',
        'source',
        'relationTypeId',
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
        const {relationTypes, refTables: {calendarTypes}, onClose, handleSubmit, fields: {from, to, relationEntities, dateNote, note, source, relationTypeId}, partyId} = this.props;
        //const {relationRoleTypes} = relationType;
        const calendars = calendarTypes ? calendarTypes.items.map(i => <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>) : null;

        let relationType = null;
        if (relationTypeId.value !== null) {
            relationType = objectById(relationTypes, relationTypeId.value);
        }

        console.log(relationType)
        const roleTypesList = relationType ? relationType.relationRoleTypes.map(i => <option value={i.id} key={i.id}>{i.name}</option>) : null;

        const submit = submitReduxForm.bind(this, RelationClassForm.validate);
        return <div className="relations-edit">
            <Form onSubmit={handleSubmit(submit)}>
                <Modal.Body>
                    {relationTypes.map(i => <Radio inline {...relationTypeId} value={i.id}>{i.name}</Radio>)}
                    {relationType && <div>
                        <div className="line datation">
                            <div className="date line">
                                {(relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL || relationType.useUnitdate == USE_UNITDATE_ENUM.ONE) && <div>
                                    <label>{i18n('party.relation.from')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" {...from.calendarTypeId}>
                                            {calendars}
                                        </FormInput>
                                        <FormInput type="text" {...from.textDate}/>
                                    </div>
                                </div>}
                                {relationType.useUnitdate == USE_UNITDATE_ENUM.INTERVAL && <div>
                                    <label>{i18n('party.relation.to')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" {...to.calendarTypeId}>
                                            {calendars}
                                        </FormInput>
                                        <FormInput type="text" {...to.textDate}/>
                                    </div>
                                </div>}
                            </div>
                        </div>
                        <FormInput type="text" label={i18n('party.relation.dateNote')}  {...dateNote} />
                        <FormInput type="text" label={i18n('party.relation.note')} {...note} />
                        <FormInput componentClass="textarea" label={i18n('party.relation.sources')} {...source} />
                        <hr/>
                        <label>{i18n('party.relation.relationEntities')}</label>

                        <div className="block entity relations">
                            <div className="relation-entities">
                                <div className="title">
                                    <label className="type">{i18n('party.relation.roleType')}</label>
                                    <label className="record">{i18n('party.relation.record')}</label>
                                </div>
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
                                    <div className="icon">
                                        <Button onClick={() => relationEntities.removeField(index)}><Icon glyph="fa-trash" /></Button>
                                    </div>
                                 </div>)}
                            </div>
                        <Button className="relation-add" onClick={() => relationEntities.addField({record:null, roleType: {id: null}})}><Icon glyph="fa-plus" /></Button>
                        </div>
                    </div>}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit">{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
        </div>;
    }
}

export default reduxForm({
    form: 'relationClassForm',
    fields: RelationClassForm.fields,
},state => ({
    refTables: state.refTables
})
)(RelationClassForm)



