import React from 'react';
import {WebApi} from 'actions/index.jsx';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, FormInput, RegistryField} from 'components/index.jsx';
import {Modal, Button, Form} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {submitReduxForm} from 'components/form/FormUtils.jsx'

import './PartyFormStyles.less';


class RelationForm extends AbstractReactComponent {

    static PropTypes = {
        relationType: React.PropTypes.object.isRequired,
    };

    static fields = [
        'from.calendarTypeId',
        'from.textDate',
        'to.calendarTypeId',
        'to.textDate',
        'dateNote',
        'note',
        'source',
        'entities[].roleTypeId',
        'entities[].record',
    ];

    static validate = (values) => {
        const errors = {};


        if(!values.from.calendarTypeId) {
            errors['from.calendarTypeId'] = i18n('party.relation.errors.undefinedCalendarType');
        }
        if(!values.to.calendarTypeId) {
            errors['to.calendarTypeId'] = i18n('party.relation.errors.undefinedCalendarType');
        }


        for (let i=0; i<values.entities.length; i++) {
            if (!values.entities[i].record) {
                errors['entities['+i+'].record'] = i18n('party.relation.errors.undefinedRecord');
            }
        }

        for (let i=0; i<values.entities.length; i++) {
            if (!values.entities[i].roleTypeId) {
                errors['entities['+i+'].roleTypeId'] = i18n('party.relation.errors.undefinedRoleType');
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
        const {relationType: {relationRoleTypes}, refTables: {calendarTypes}, onClose, handleSubmit, fields: {from, to, entities, dateNote, note, source}} = this.props;
        const calendars = calendarTypes ? calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>}) : null;
        const roleTypesList = relationRoleTypes ? relationRoleTypes.map(i=> {return <option value={i.id} key={i.id}>{i.name}</option>}) : null;

        const submit = submitReduxForm.bind(this, RelationForm.validate);
        return <div className="relations-edit">
            <Form onSubmit={handleSubmit(submit)}>
                <Modal.Body>
                    <div className="line datation">
                        <div className="date line">
                            <div>
                                <label>{i18n('party.relation.from')}</label>
                                <div className="line">
                                    <FormInput componentClass="select" {...from.calendarTypeId}>
                                        {calendars}
                                    </FormInput>
                                    <FormInput type="text" {...from.textDate}/>
                                </div>
                            </div>
                            <div>
                                <label>{i18n('party.relation.to')}</label>
                                <div className="line">
                                    <FormInput componentClass="select" {...to.calendarTypeId}>
                                        {calendars}
                                    </FormInput>
                                    <FormInput type="text" {...to.textDate}/>
                                </div>
                            </div>
                        </div>
                    </div>
                    <FormInput type="text" label={i18n('party.relation.dateNote')}  {...dateNote} />
                    <FormInput type="text" label={i18n('party.relation.note')} {...note} />
                    <FormInput componentClass="textarea" label={i18n('party.relation.sources')} {...source} />
                    <hr/>
                    <label>{i18n('party.relation.entities')}</label>

                    <div className="block entity relations">
                        <div className="relation-entities">
                            <div className="title">
                                <label className="type">{i18n('party.relation.roleType')}</label>
                                <label className="record">{i18n('party.relation.record')}</label>
                            </div>

                            {entities.map((i,index) => <div className="relation-row" key={index}>
                                <div className="type">
                                    <FormInput componentClass="select" {...i.roleTypeId}>
                                        <option key={0}/>
                                        {roleTypesList}
                                    </FormInput>
                                </div>
                                <div className="record">
                                    <RegistryField {...i.record}/>
                                </div>
                                <div className="icon">
                                    <Button onClick={() => entities.removeField(index)}><Icon glyph="fa-trash" /></Button>
                                </div>
                             </div>)}
                        </div>
                    <Button className="relation-add" onClick={() => entities.addField({record:null, roleTypeId: null})}><Icon glyph="fa-plus" /></Button>
                    </div>
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
    form: 'relationForm',
    fields: RelationForm.fields,
},state => ({
    refTables: state.refTables
})
)(RelationForm)



