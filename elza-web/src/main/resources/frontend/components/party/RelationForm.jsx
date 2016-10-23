import React from 'react';
import {WebApi} from 'actions/index.jsx';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, Autocomplete, i18n, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button, Glyphicon, Form} from 'react-bootstrap'
import {indexById} from 'stores/app/utils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {refRegistryListFetchIfNeeded} from 'actions/refTables/registryRegionList.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {Combobox} from 'react-input-enhancements'

import './PartyFormStyles.less';

/**
  * RELATION FORM
  * *********************************************
  * Formulář vztahu osoby
  */ 
class RelationForm extends AbstractReactComponent {
    state = {                                      // ve state jsou uložena a průběžně udržová data formuláře
        data : this.props.initData,                     // předvyplněná data formuláře
        errors: [],                                      // sezn chyb k vypsání uživateli
        recordList: []

    };

    componentWillReceiveProps(nextProps) {
        this.dispatch(calendarTypesFetchIfNeeded());        // budeme potřebovat seznam typů kaledáře, tj pokud není ještě načtený, se načte
        this.dispatch(refPartyTypesFetchIfNeeded());        // budeme potřebovt také seznam typů osob
        this.dispatch(refRegistryListFetchIfNeeded());      // a budeme potřebovat potřebovat seznam rejstříkových položek

        let data = this.state.data;
        const types = this.initRelationTypes(nextProps.initData.relationTypeId, nextProps);
        data =  Object.assign({}, data, {...types});
        this.setState({
            data: data
        });
    }

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());        // budeme potřebovat seznam typů kaledáře, tj pokud není ještě načtený, se načte
        this.dispatch(refPartyTypesFetchIfNeeded());        // budeme potřebovt také seznam typů osob
        this.dispatch(refRegistryListFetchIfNeeded());      // a budeme potřebovat potřebovat seznam rejstříkových položek


        let data = this.state.data;
        const types = this.initRelationTypes(this.props.initData.relationTypeId, this.props);
        data =  Object.assign({}, data, {...types});
        this.setState({
            data: data
        });
    }

     /**
     * ADD ENTITY
     * *********************************************
     * přidání nové prázdné entity do vztahu
     */ 
    addEntity = () => {
        const data = this.state.data;                         // původní data vztahu(formuláře)
        data.entities[data.entities.length]={               // pridání nové prázdné entity na konec seznamu entit
            record: null,
            roleTypeId: null,
        };
        this.setState({
            data : data                                     // uložení výsledku do state
        });
    };

    /**
     * Vrací možné typy vztahů a typ rolí. Provede prvotní předvybrání typu vztahu.
     * @param relationTypeId vybraný typ vztahu
     * @param nextProps properties
     * @returns {{roleTypes: Array, relationTypes: Array, relationTypeId: *}}
     */
    initRelationTypes = (relationTypeId, nextProps) => {
        let allRelationTypes = [];
        for(let i=0; i<nextProps.refTables.partyTypes.items.length; i++) {
            if(nextProps.refTables.partyTypes.items[i].partyTypeId == nextProps.initData.partyTypeId) {
                allRelationTypes = nextProps.refTables.partyTypes.items[i].relationTypes;
            }
        }

        let roleTypes = [];
        const relationTypes = [];

        if(allRelationTypes) {
            var classType = nextProps.initData.classType;
            for(let i=0; i<allRelationTypes.length; i++) {
                if(allRelationTypes[i].classType === classType) {
                    relationTypes.push(allRelationTypes[i]);
                }
            }
        }

        var selectedRelationTypeId = relationTypeId;
        if(selectedRelationTypeId == undefined) {
            selectedRelationTypeId = relationTypes && relationTypes[0] ? relationTypes[0].relationTypeId : null;
        }

        relationTypes.map(rt => {
           if(rt.relationTypeId == selectedRelationTypeId) {
               roleTypes = rt.relationRoleTypes;
           }
        });

        return {
            roleTypes: roleTypes,
            relationTypes: relationTypes,
            relationTypeId: selectedRelationTypeId
        };
    };

     /**
     * REMOVE ENTITY
     * *********************************************
     * odstranění jedné entity ze vztahu
     * @params int index - lokální index entity, kterou odstranit 
     * @params event - událost která změnu vyvolala
     */ 
    removeEntity = (index, event) => {
        const data = this.state.data;                                 // původní data formuláře
        const entities = [];                                          // nový seznnam entit
        for(let i=0; i<data.entities.length; i++) {                  // procházejí se původní entity
            if(i != index) {                                         // a všechny co nejsou mazaná entita
               entities[entities.length] = data.entities[i];        // přidáme do nových entit
            }
        }
        data.entities = entities;                                   // stare entity v datech vymeníme za nové
        this.setState({
            data : data                                             // a uložíme nová data do state
        });
    };

    /**
     * UPDATE VALUE
     * *********************************************
     * aktualizace nějaké hodnoty ve formuláři (kromě entit)
     * @params event - událost která změnu vyvolala
     */
    updateValue = (event) => {
        const value = event.target.value;                                                 // hodnota změněného pole formuláře
        const variable = event.target.name;                                               // nazeb měněné hodnoty
        const data = this.state.data;                                                     // puvodni data formuláře
        switch(variable) {
            case "relationTypeId" : {
                data.relationTypeId = value;
                data.entities.map((i,index) =>{
                    data.entities[index].roleTypeId = null;
                    data.entities[index].record = null;
                });

                var types = this.initRelationTypes(data.relationTypeId, this.props);
                data.roleTypes = types.roleTypes;
                data.relationTypes = types.relationTypes;

                break;
            }
            case "note" : data.note = value; break;                        // změna poznámky
            case "dateNote" : data.dateNote = value; break;                // změna poznámky k času
            case "source" : data.source = value; break;
            case "fromText" : data.from.textDate = value; break;           // změna data od
            case "toText" : data.to.textDate = value; break;               // změna data do
            case "fromCalendar" : data.from.calendarTypeId = value; break; // změna typu kalendáře od
            case "toCalendar" : data.to.calendarTypeId = value; break;     // změna typu kalendáře do
        }
        this.setState({
            data : data                                                                 // uložení změn do state
        });
    };

     /**
     * UPDATE ENTITY VALUE
     * *********************************************
     * aktualizace nějaké hodnoty entity ve formuláři 
     * @params obj entity - obsahuje index entity a měněnou hodnotu, např {index 5, variable: 'record'} 
     * @params event - událost která změnu vyvolala
     */   
    updateEntityValue = (entity, event) => {
        const data = this.state.data;                             // puvodní data formuláře
        for(let i=0; i<data.entities.length; i++) {              // procházejí se všechny entity
            if(i == entity.index) {                              // nalezení té pravé, krou máme změnit
                switch(entity.variable) {                        
                    case "record" : data.entities[entity.index].record = {...event}; break;
                    case "role" : {
                        data.entities[entity.index].roleTypeId = event.target.value;
                        data.entities[entity.index].record = null;
                        break;
                    }
                }
            }
        }
        this.setState({
            data : data                                         // uložení změny do state
        });
    };


    /**
     * HANDLE SUBMIT
     * *********************************************
     * Odeslání formuláře
     */
    handleSubmit = (e) => {
        e.preventDefault();
        const errors = this.validate();               // seznam  chyb ve vyplněných datech
        if(errors.length > 0) {                      // pokud je formulář chybně vyplnění
            this.setState({             
                errors : errors                     // seznam chyb se uloží do state => dojde s přerenderování, při kterém budou chyby vypsany
            });
        }else{                                      // formulář je vyplněn dobře
            this.props.onSave(this.state.data);     // vyplněná data se pošlou do funkce definované nadřazenou komponentou v proměnné onSave 
        }
    };


    /**
     * VALIDATE
     * *********************************************
     * Kontrola vyplnění formuláře vztahu
     * @return array errors - seznam chyb 
     */
    validate = () => {
        const errors = [];                                        // seznam chyb
        const data = this.state.data;                             // zadaná data z formuláře

        //kontrola vyplnění typu vztahu
        if(data.relationTypeId == 0 || data.relationTypeId == null ||  data.relationTypeId == undefined) {
            errors[errors.length] = i18n('party.relation.errors.undefinedRelationType');
        }

        //oba typy kalendáře musí být zadané
        if(
            data.from.calendarTypeId == 0 || data.from.calendarTypeId == null ||  data.from.calendarTypeId == undefined || 
            data.to.calendarTypeId == 0 || data.to.calendarTypeId == null ||  data.to.calendarTypeId == undefined 
        ) {
            errors[errors.length] = i18n('party.relation.errors.undefinedCalendarType');
        }


        for(let i=0; i<data.entities.length; i++) {
            if(data.entities[i].record == undefined || data.entities[i].record.id == undefined) {
                errors[errors.length] = i18n('party.relation.errors.undefinedRecord');
                break;
            }
        }

        for(let i=0; i<data.entities.length; i++) {
            if(data.entities[i].roleTypeId ==0 || data.entities[i].roleTypeId == null ||  data.entities[i].roleTypeId == undefined) {
                errors[errors.length] = i18n('party.relation.errors.undefinedRoleType');
                break;
            }
        }

        return errors;                                          // vrácení seznamu chyb
    }

    /**
     * HANDLE CLOSE
     * *********************************************
     * Zavření dialogového okénka formuláře
     */
    handleClose = () => {
        this.dispatch(modalDialogHide());
    };

    renderRecord = (item, isHighlighted, isSelected) => {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (<div className={cls} key={item.id} >
            <div className="name" title={item.record}>{item.record}</div>
            <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
        </div>)
    };

    handleSearchChange = (roleTypeId, text) => {
        const partyId = this.props.initData.partyId;
        text = text == "" ? null : text;

        WebApi.findRecordForRelation(text,roleTypeId, partyId)
                .then(json => {
                    this.setState({
                        recordList: json.recordList.map(record => {
                            return {
                                id: record.id,
                                record: record.record,
                                characteristics: record.characteristics
                            }
                        })
                    })
                })
    };


    /**
     * RENDER
     * *********************************************
     * Vykreslení formuláře
     */
    render() {

        const roleTypes = this.state.data.roleTypes;
        const relationTypes = this.state.data.relationTypes;

        // zaznamy pro autocomplate
        const records = [];
        for(let i = 0; i<this.props.refTables.registryRegionList.items.recordList.length; i++) {
            records[records.length] = {
                id: this.props.refTables.registryRegionList.items.recordList[i].recordId,
                name: this.props.refTables.registryRegionList.items.recordList[i].record,
            }
        }

        var selectedRelationTypeId = this.state.data.relationTypeId;
        var typeUnselected = this.state.data.relationTypeId == undefined;

        return (
            <div className="relations-edit">
                <Form onSubmit={this.handleSubmit}>
                    <Modal.Body>
                        <ul className="errors">
                            {this.state.errors.map((i, index)=> {return <li key={index}>{i}</li>})}
                        </ul>
                        <FormInput disabled={this.state.data.relationId != undefined}
                                     componentClass="select" label={i18n('party.relation.type')} name="relationTypeId" value={selectedRelationTypeId} onChange={this.updateValue}>
                            <option value="0" key="0"/>
                            {relationTypes ? relationTypes.map(i=> {return <option value={i.relationTypeId} key={i.relationTypeId}>{i.name}</option>}) : null}
                        </FormInput>
                        <hr/>
                        <div className="line datation">
                            <div className="date line">
                                <div>
                                    <label>{i18n('party.relation.from')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" name="fromCalendar" value={this.state.data.from.calendarTypeId} onChange={this.updateValue} >
                                            <option value={0} key={0}/>
                                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                        </FormInput>
                                        <FormInput type="text"  name="fromText" value={this.state.data.from.textDate} onChange={this.updateValue} />
                                    </div>
                                </div>
                                <div>
                                    <label>{i18n('party.relation.to')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" name="toCalendar" value={this.state.data.to.calendarTypeId} onChange={this.updateValue} >
                                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                        </FormInput>
                                        <FormInput type="text" name="toText" value={this.state.data.to.textDate} onChange={this.updateValue} />
                                    </div>
                                </div>
                            </div>
                        </div>
                        <FormInput type="text" label={i18n('party.relation.dateNote')}  name="dateNote" value={this.state.data.dateNote} onChange={this.updateValue} />
                        <FormInput type="text" label={i18n('party.relation.note')} name="note" value={this.state.data.note} onChange={this.updateValue} />
                        <FormInput componentClass="textarea" label={i18n('party.relation.sources')} name="source" value={this.state.data.source} onChange={this.updateValue} />
                        <hr/>
                        <label>{i18n('party.relation.entities')}</label>

                        <div className="block entity relations">
                            <div className="relation-entities">
                                <div className="title">
                                    <label className="type">{i18n('party.relation.roleType')}</label>
                                    <label className="record">{i18n('party.relation.record')}</label>
                                    <label className="icon"></label>
                                </div>
                                <div>
                                    {this.state.data.entities.map((j,index)=> {
                                        var roleTypeUnselected = j.roleTypeId == undefined || j.roleTypeId == 0;
                                        return <div className="relation-row" key={index}>
                                                    <div className="type">
                                                <FormInput componentClass="select" disabled={typeUnselected} value={j.roleTypeId} onChange={this.updateEntityValue.bind(this, {index:index, variable: 'role'})}>
                                                    <option value={0} key={0}/>
                                                    {roleTypes ? roleTypes.map(i=> {return <option value={i.roleTypeId} key={i.roleTypeId}>{i.name}</option>}) : null}
                                                </FormInput>
                                                    </div>
                                                    <div className="record">
                                                     <Autocomplete
                                                        disabled={roleTypeUnselected}
                                                        customFilter
                                                        className='autocomplete-record flex-grow-1'
                                                        value={j.record}
                                                        items={this.state.recordList}
                                                        getItemId={(item) => item ? item.id : null}
                                                        getItemName={(item) => item ? item.record : ''}
                                                        onSearchChange={text => {this.handleSearchChange(j.roleTypeId, text) }}
                                                        onChange={(id,valObj) =>{this.updateEntityValue({index:index, variable: 'record'}, valObj)}}
                                                        renderItem={this.renderRecord}
                                                         />
                                                    </div>
                                                    <div className="icon">
                                                        <Button onClick={this.removeEntity.bind(this, index)}><Icon glyph="fa-trash" /></Button>
                                                    </div>
                                                 </div>
                                    })}
                                </div>

                            </div>
                        <Button className="relation-add" onClick={this.addEntity}><Icon glyph="fa-plus" /></Button>
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={this.handleClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </Form>
            </div>
        )
    }

    /**
     * GET GREGORIAN CALENDAR ID
     * *********************************************
     * Získání identifikátorů gregoriánského kalendáře, který bude výchozí
     */
     getGregorianCalendarId() {
        let id = null;
        for(let i = 0; i<this.props.refTables.calendarTypes.items.length; i++) {
            if(this.props.refTables.calendarTypes.items[i].code == "GREGORIAN") {
                id = this.props.refTables.calendarTypes.items[i].id;
            }
        }
        return id;
     }
}



function matchStateToTerm (state, value) {
  return (
    state.name.toLowerCase().indexOf(value.toLowerCase()) !== -1 
  )
}

export default reduxForm({
    form: 'relationForm',
    fields: [],
},state => ({
    initialValues: state.form.relationForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'relationForm', data})}
)(RelationForm)



