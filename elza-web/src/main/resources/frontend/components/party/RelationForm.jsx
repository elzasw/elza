/**
 * Formulář přidání nového vztahu k osobě
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Search, i18n} from 'components';
import {Modal, Button, Input, Glyphicon} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {refRegistryListFetchIfNeeded} from 'actions/refTables/registryList'


require ('./PartyFormStyles.less');

const validate = (values, props) => {
    const errors = {};
    if ((!values.relationTypeId || values.relationTypeId==0 )) {
        errors.relationTypeId = i18n('global.validation.required');
    }
    if ((!values.calendarTypeIdTo || values.calendarTypeIdTo==0 )) {
        errors.calendarTypeIdTo = i18n('global.validation.required');
    }

    if ((!values.calendarTypeIdFrom || values.calendarTypeIdFrom==0 )) {
        errors.calendarTypeIdFrom = i18n('global.validation.required');
    }
    console.log(values.entities);
    return errors;
};

var RelationForm = class RelationForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.dispatch(refRegistryListFetchIfNeeded());
        this.state = {
            entities : props.initData.entities
        };
        this.props.load(props.initData);
        this.bindMethods(
            'addEntity',
            'removeEntity',
            'changeEntityValue'
        );
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount(){
    }

    addEntity(){
        var entities = this.state.entities;
        entities[entities.length]={
            recordId: null,
            roleTypeId: null,
            sources: '',
        }
        this.setState({
            entities : entities
        });
    }

    removeEntity(index, event){
        var entities = [];
        
        for(var i=0; i<this.state.entities.length; i++){
            if(i != index){
               entities[entities.length] = this.state.entities[i]; 
            }
        }
        this.setState({
            entities : entities
        });
    }

    
    changeEntityValue(data, event){
        var entities = this.state.entities;
        for(var i=0; i<this.state.entities.length; i++){
            if(i == data.index){
                switch(data.variable){
                    case "record" : entities[data.index].recordId = event.target.value; break;
                    case "role" : entities[data.index].roleTypeId = event.target.value; break;
                    case "sources" : entities[data.index].sources = event.target.value; break;
                }
            }
        }
        this.setState({
            entities : entities
        });
    }

    handleUpdateValue(e){
        var value = e.target.value;
        var variable = e.target.name;
        var party = this.state.party;   
        this.dispatch(updateParty(party));
    }

    render() {
        console.log(this.props.refTables);
        var relationTypes = [];
        for(var i=0; i<this.props.refTables.partyTypes.items.length; i++){
            if(this.props.refTables.partyTypes.items[i].partyTypeId == this.props.initData.partyTypeId){
                relationTypes = this.props.refTables.partyTypes.items[i].relationTypes;
            }
        }
        var roleTypes = [];
        for(var i=0; i<relationTypes.length; i++){
            if(relationTypes[i].relationTypeId == this.props.initData.relationTypeId){
                roleTypes = relationTypes[i].relationRoleTypes;
            }
        }
        const {fields: {relationId, relationTypeId, dateFrom, dateTo, calendarTypeIdFrom, calendarTypeIdTo, note, dateNote, entities}, handleSubmit, onClose} = this.props;
        var entities2 = [];
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <input type="hidden" {...relationId}/>
                        <Input type="select" label={i18n('party.relation.type')} {...relationTypeId} {...decorateFormField(relationTypeId)}>
                            <option value="0" key="0"></option> 
                            {relationTypes.map(i=> {return <option value={i.relationTypeId} key={i.relationTypeId}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('party.relation.note')} {...note} {...decorateFormField(note)} />
                        <hr/>
                        <div className="line">
                            <Input type="text" label={i18n('party.relation.from')} {...dateFrom} {...decorateFormField(dateFrom)} />
                            <Input type="select" label={i18n('party.calendarTypeFrom')} {...calendarTypeIdFrom} {...decorateFormField(calendarTypeIdFrom)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
                            </Input>   
                        </div>
                        <div className="line">
                            <Input type="text" label={i18n('party.relation.to')} {...dateTo} {...decorateFormField(dateTo)} /> 
                            <Input type="select" label={i18n('party.calendarTypeTo')} {...calendarTypeIdTo} {...decorateFormField(calendarTypeIdTo)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
                            </Input>
                        </div>
                        <Input type="text" label={i18n('party.relation.dateNote')} {...dateNote} {...decorateFormField(dateNote)} />
                        <hr/>
                        <h5>{i18n('party.relation.entities')}</h5>
                        <div>
                            {this.state.entities.map((j,index)=> {return <div className="block entity">
                                <Input type="select" label={i18n('party.relation.record')} value={j.record} onChange={this.changeEntityValue.bind(this, {index:index, variable: 'record'})}>
                                    <option value={0} key={0}></option> 
                                    {this.props.refTables.registryList.items.recordList.map(i=> {return <option key={i.recordId} value={i.recordId}>{i.record}</option>})}
                                </Input> 
                                <Input type="select" label={i18n('party.relation.roleType')} value={j.roleTypeId} onChange={this.changeEntityValue.bind(this, {index:index, variable: 'role'})}>
                                    <option value={0} key={0}></option> 
                                    {roleTypes ? roleTypes.map(i=> {return <option value={i.roleTypeId} key={i.roleTypeId}>{i.name}</option>}) : ''}
                                </Input>
                                <Input type="text" label={i18n('party.relation.sources')} value={j.sources} onChange={this.changeEntityValue.bind(this, {index:index, variable: 'sources'})}/>
                                <div className="ico">
                                    <Button onClick={this.removeEntity.bind(this, index)}><Glyphicon glyph="trash" /></Button>
                                </div> 
                            </div>})}
                        </div>   
                        <hr/>
                        <Button onClick={this.addEntity}><Glyphicon glyph="plus" /></Button>
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
    form: 'relationForm',
    fields: ['relationId','relationTypeId', 'dateFrom', 'dateTo', 'calendarTypeIdFrom', 'calendarTypeIdTo', 'note', 'dateNote', 'entities'],
    validate
},state => ({
    initialValues: state.form.relationForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'relationForm', data})}
)(RelationForm)



