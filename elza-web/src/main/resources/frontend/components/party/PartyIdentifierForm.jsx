/**
 * Formulář přidání nového identifikátoru osobě
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button, Glyphicon} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refPartyListFetchIfNeeded} from 'actions/refTables/partyList.jsx'

/**
 * PARTY IDENTIFIER FORM
 * *********************************************
 * formulář identifikátoru osoby
 */
const PartyIdentifierForm = class PartyIdentifierForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

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

    componentDidMount() {
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
    }

    componentWillReceiveProps() {
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
    }

    /**
     * UPDATE VALUE
     * *********************************************
     * aktualizace nějaké hodnoty ve formuláři
     * @params event - událost která změnu vyvolala
     */
    updateValue(event) {
        const value = event.target.value;                                                     // hodnota změněného pole formuláře
        const variable = event.target.name;                                                   // nazeb měněné hodnoty
        const data = this.state.data;                                                         // puvodni data formuláře
        switch(variable) {
            case "source" : data.source = value; break;                        // změna zdrojů dat
            case "note" : data.note = value; break;                            // změna poznámky
            case "identifier" : data.identifier = value; break;                // změna názvu identifikátoru
            case "fromText" : data.from.textDate = value; break;               // změna data od
            case "toText" : data.to.textDate = value; break;                   // změna data do
            case "fromCalendar" : data.from.calendarTypeId = value; break;     // změna typu kalendáře od
            case "toCalendar" : data.to.calendarTypeId = value; break;         // změna typu kalendáře do
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
    validate() {
        let errors = [];                                        // seznam chyb
        const {data} = this.state;                             // zadaná data z formuláře

        //kontrola vyplnění názvu identifikátoru
        if(data.identifier == "" || data.identifier == null ||  data.identifier == undefined) {
            errors[errors.length] = i18n('party.identifier.errors.undefinedIdentifierText');
        }

        return errors;                                          // vrácení seznamu chyb
    }

    /**
     * HANDLE CLOSE
     * *********************************************
     * Zavření dialogového okénka formuláře
     */
    handleClose() {
        this.dispatch(modalDialogHide());
    }

    /**
     * HANDLE SUBMIT
     * *********************************************
     * Odeslání formuláře
     */
    handleSubmit(e) {
        const errors = this.validate();               // seznam  chyb ve vyplněných datech
        if(errors.length > 0) {                      // pokud je formulář chybně vyplnění
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
        const {refTables: {calendarTypes}} = this.props;
        const {errors, data} = this.state;
        return (
            <div>
                <Modal.Body>
                    <ul className="errors">
                        {errors.map((i, b) => <li key={b}>{i}</li>)}
                    </ul>
                    <form>
                        <FormInput type="text" label={i18n('party.identifier.source')} name="source" value={data.source} onChange={this.updateValue} />
                        <FormInput type="text" label={i18n('party.identifier.identifierText')} name="identifier" value={data.identifier} onChange={this.updateValue} />
                        <hr/>
                        <div className="line datation">
                            <div className="date line">
                                <div>
                                    <label>{i18n('party.identifier.from')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" name="fromCalendar" value={data.from.calendarTypeId} onChange={this.updateValue} >
                                            {calendarTypes.items.map(i=> <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>)}
                                        </FormInput>
                                        <FormInput type="text"  name="fromText" value={data.from.textDate} onChange={this.updateValue} />
                                    </div>
                                </div>
                                <div>
                                    <label>{i18n('party.identifier.to')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" name="toCalendar" value={data.to.calendarTypeId} onChange={this.updateValue} >
                                            {calendarTypes.items.map(i=> <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>)}
                                        </FormInput>
                                        <FormInput type="text" name="toText" value={data.to.textDate} onChange={this.updateValue} />
                                    </div>
                                </div>
                            </div>
                        </div>
                        <hr/>
                        <FormInput type="text" label={i18n('party.identifier.note')} name="note" value={data.note} onChange={this.updateValue} />
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



