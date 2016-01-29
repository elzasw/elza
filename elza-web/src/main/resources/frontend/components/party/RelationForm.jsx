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
    return errors;
};

var RelationForm = class RelationForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(calendarTypesFetchIfNeeded());
        this.state = {
            entities : props.initData.entities
        };
        this.props.load(props.initData);
        this.bindMethods(
            'addEntity',
            'removeEntity'
        );
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount(){
    }

    addEntity(){
        var entities = this.state.entities;
        entities[entities.length]={
            record: "ab",
            roleType: 1
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

    render() {
        const {fields: {relationClassId, relationTypeId, dateFrom, dateTo, calendarTypeIdFrom, calendarTypeIdTo, note, entities}, handleSubmit, onClose} = this.props;
        var entities2 = [];
        return (
            <div>
                <Modal.Body>
                    <form onSubmit={handleSubmit}>
                        <div className="line">
                            <Input type="select" label={i18n('party.relation.class')} {...relationClassId} {...decorateFormField(relationClassId)}>
                                <option value="0" key="0"></option> 
                                <option value="1" key="1">Vznik</option> 
                            </Input>
                            <Input type="select" label={i18n('party.relation.type')} {...relationTypeId} {...decorateFormField(relationTypeId)}>
                                <option value="0" key="0"></option> 
                                <option value="1" key="1">členství</option> 
                            </Input>
                        </div>                            
                        <div className="line">
                            <Input type="text" label={i18n('party.relation.from')} {...dateFrom} {...decorateFormField(dateFrom)} />
                            <Input type="select" label={i18n('party.calendarTypeFrom')} {...calendarTypeIdFrom} {...decorateFormField(calendarTypeIdFrom)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id}>{i.name}</option>})}
                            </Input>   
                        </div>
                        <div className="line">
                            <Input type="text" label={i18n('party.relation.to')} {...dateTo} {...decorateFormField(dateTo)} /> 
                            <Input type="select" label={i18n('party.calendarTypeTo')} {...calendarTypeIdTo} {...decorateFormField(calendarTypeIdTo)}>
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id}>{i.name}</option>})}
                            </Input>
                        </div>
                        <Input type="text" label={i18n('party.relation.note')} {...note} {...decorateFormField(note)} />
                        <h5>{i18n('party.relation.entities')}</h5>
                        <div>
                            {this.state.entities.map((i,index)=> {return <div className="block entity">
                                <Input type="select" label={i18n('party.relation.record')} value={i.record} >
                                    <option value="0" key="0"></option> 
                                    <option value="1" key="1">Záznam 1</option> 
                                </Input> 
                                <Input type="select" label={i18n('party.relation.roleType')} value={i.roleTypeId}>
                                    <option value="0" key="0"></option> 
                                    <option value="1" key="1">Role 1</option> 
                                </Input> 
                                <Input type="textarea" label={i18n('party.relation.sources')} value={i.sources}/>
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
    fields: ['relationClassId', 'relationTypeId', 'dateFrom', 'dateTo', 'calendarTypeIdFrom', 'calendarTypeIdTo', 'note', 'entities'],
    validate
},state => ({
    initialValues: state.form.relationForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'relationForm', data})}
)(RelationForm)



