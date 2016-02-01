/**
 * Formulář přidání nového identifikátoru osobě
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input, Glyphicon} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'

/**
 * PARTY IDENTIFIER FORM
 * *********************************************
 * formulář identifikátoru osoby
 */
var PartyIdentifierForm = class PartyIdentifierForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.state = {                                      // ve state jsou uložena a průběžně udržová data formuláře
            data : this.props.initData,                     // předvyplněná data formuláře
            errors: []                                      // sezn chyb k vypsání uživateli
        };
        this.bindMethods(                                   // pripojení potřebných metod - aby měly k dispozici tento objekt (formulář)
            'updateValue',                                  // funkce pro změnu nějaké hodnoty identifikátoru
            'validate',                                     // funkce pro kontrolu zadaných dat formuláře
            'handleClose',                                  // funkce pro zavření okna dialogu
            'handleSubmit'                                  // funkce pro odeslání formuláře
        );
    }

    /**
     * UPDATE VALUE
     * *********************************************
     * aktualizace nějaké hodnoty ve formuláři
     * @params event - událost která změnu vyvolala
     */
    updateValue(event){
        var value = event.target.value;                                                     // hodnota změněného pole formuláře
        var variable = event.target.name;                                                   // nazeb měněné hodnoty
        var data = this.state.data;                                                         // puvodni data formuláře
        switch(variable){
            case "source" : data.source = event.target.value; break;                        // změna zdrojů dat
            case "note" : data.note = event.target.value; break;                            // změna poznámky
            case "identifier" : data.identifier = event.target.value; break;                // změna názvu identifikátoru
            case "fromText" : data.from.textDate = event.target.value; break;               // změna data od
            case "toText" : data.from.textDate = event.target.value; break;                 // změna data do
            case "fromCalendar" : data.from.calendarTypeId = event.target.value; break;     // změna typu kalendáře od
            case "toCalendar" : data.to.calendarTypeId = event.target.value; break;         // změna typu kalendáře do
        }
        this.setState({
            data : data                                                                     // uložení změn do state
        });
    }

    /**
     * VALIDATE
     * *********************************************
     * Kontrola vyplnění formuláře identifikátoru
     * @return array errors - seznam chyb 
     */
    validate(){
        var errors = [];                                        // seznam chyb
        var data = this.state.data;                             // zadaná data z formuláře

        //kontrola vyplnění názvu identifikátoru
        if(data.identifier == "" || data.identifier == null ||  data.identifier == undefined){
            errors[errors.length] = i18n('party.identifier.errors.undefinedIdentifierText');
        }

        //oba typy kalendáře musí být zadané
        if(
            data.from.calendarTypeId == 0 || data.from.calendarTypeId == null ||  data.from.calendarTypeId == undefined || 
            data.to.calendarTypeId == 0 || data.to.calendarTypeId == null ||  data.to.calendarTypeId == undefined 
        ){
            errors[errors.length] = i18n('party.name.errors.undefinedCalendarType');
        }

        return errors;                                          // vrácení seznamu chyb
    }

   /**
     * HANDLE CLOSE
     * *********************************************
     * Zavření dialogového okénka formuláře
     */
    handleClose(){
        this.dispatch(modalDialogHide());
    }

    /**
     * HANDLE SUBMIT
     * *********************************************
     * Odeslání formuláře
     */
    handleSubmit(e){
        var errors = this.validate();               // seznam  chyb ve vyplněných datech
        if(errors.length > 0){                      // pokud je formulář chybně vyplnění
            this.setState({             
                errors : errors                     // seznam chyb se uloží do state => dojde s přerenderování, při kterém budou chyby vypsany
            });
        }else{                                      // formulář je vyplněn dobře
            this.props.onSave(this.state.data);     // vyplněná data se pošlou do funkce definované nadřazenou komponentou v proměnné onSave 
        }
    }


    /**
     * RENDER
     * *********************************************
     * Vykreslení formuláře
     */
    render() {
        return (
            <div>
                <Modal.Body>
                    <ul className="errors">
                        {this.state.errors.map(i=> {return <li>{i}</li>})}
                    </ul>
                    <form>
                        <Input type="text" label={i18n('party.identifier.identifierText')} name="identifier" value={this.state.data.identifier} onChange={this.updateValue} />
                        <Input type="text" label={i18n('party.identifier.note')} name="note" value={this.state.data.note} onChange={this.updateValue} />
                        <Input type="text" label={i18n('party.identifier.source')} name="source" value={this.state.data.source} onChange={this.updateValue} />
                        <div className="line">
                            <Input type="text" label={i18n('party.identifier.from')} name="fromText" value={this.state.data.from.textDate} onChange={this.updateValue} />
                            <Input type="select" label={i18n('party.calendarTypeFrom')} name="fromCalendar" value={this.state.data.from.calendarTypeId} onChange={this.updateValue} >
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
                            </Input>   
                        </div>
                        <div className="line">
                            <Input type="text" label={i18n('party.identifier.to')} name="toText" value={this.state.data.to.textDate} onChange={this.updateValue} />
                            <Input type="select" label={i18n('party.calendarTypeTo')} name="toCalendar" value={this.state.data.to.calendarTypeId} onChange={this.updateValue} >
                                <option value="0" key="0"></option> 
                                {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name}</option>})}
                            </Input>
                        </div>
                    </form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={this.handleClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = reduxForm({
    form: 'PartyIdentifierForm',
    fields: [],
},state => ({
    initialValues: state.form.partyIdentifierForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'partyIdentifierForm', data})}
)(PartyIdentifierForm)



