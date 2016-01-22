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

require ('./PartyFormStyles.less');

const validate = (values, props) => {
    const errors = {};
    if ((!values.relationTypeId || values.relationTypeId==0 )) {
        errors.relationTypeId = i18n('global.validation.required');
    }
    return errors;
};

var AddRelationForm = class AddRelationForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        //this.dispatch(refPartyNameFormTypesFetchIfNeeded());
        //this.dispatch(calendarTypesFetchIfNeeded());
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
        const {fields: {relationClassId, relationTypeId, dateFrom, dateTo, note, sources, entities}, handleSubmit, onClose} = this.props;
        var entities2 = [];
        //entities.initialValue.map(i=> {alert("a")});
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
                            <Input type="text" label={i18n('party.relation.from')} {...dateFrom} {...decorateFormField(dateFrom)} />
                            <Input type="text" label={i18n('party.relation.to')} {...dateTo} {...decorateFormField(dateTo)} />      
                        </div>
                        <Input type="textarea" label={i18n('party.relation.note')} {...note} {...decorateFormField(note)} />
                        <Input type="textarea" label={i18n('party.relation.sources')} {...sources} {...decorateFormField(sources)} />
                        <h5>{i18n('party.relation.entities')}</h5>
                        <div>
                            {this.state.entities.map((i,index)=> {return <div className="line entity">
                                <div className="column">
                                    <strong>{i.record}</strong><br/>
                                    {i.roleType}
                                </div>
                                <div className="column ico">
                                    <Button onClick={this.removeEntity.bind(this, index)}><Glyphicon glyph="trash" /></Button>
                                </div> 
                            </div>})}
                        </div>
                        <input type="hidden" name="entities" value='{JSON.stringify(this.state.entities)}' />
                        <Search/>
                        <Input type="select" label={i18n('party.relation.class')}>
                            <option value="0" key="0"></option> 
                            <option value="1" key="1">Vznik</option> 
                        </Input>     
                        <hr/>
                        <Button onClick={this.addEntity}><Glyphicon glyph="plus" /></Button>
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
    form: 'addRelationForm',
    fields: ['relationClassId', 'relationTypeId', 'dateFrom', 'dateTo', 'note', 'sources', 'entities'],
    validate
},state => ({
    initialValues: state.form.addRelationForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addRelationForm', data})}
)(AddRelationForm)



