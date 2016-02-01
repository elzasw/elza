/**
 * Formulář autora osoby
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/actionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input, Glyphicon} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {refPartyListFetchIfNeeded} from 'actions/refTables/partyList'

/**
 * PARTY CREATOR FORM
 * *********************************************
 * formulář autora osoby
 */
var PartyCreatorForm = class PartyCreatorForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
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
            case "creatorId" : data.creatorId = event.target.value; break;                  // identifikátor autora osoby
        }
        this.setState({
            data : data                                                                     // uložení změn do state
        });
    }

    /**
     * VALIDATE
     * *********************************************
     * Kontrola vyplnění formuláře autora
     * @return array errors - seznam chyb 
     */
    validate(){
        var errors = [];                                        // seznam chyb
        var data = this.state.data;                             // zadaná data z formuláře

        //kontrola vyplnění autora
        if(data.creatorId == 0 || data.creatorId == null ||  data.creatorId == undefined){
            errors[errors.length] = i18n('party.creator.errors.undefinedCreator');
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
                        
                        <Input type="select" label={i18n('party.creator.creator')} name="creatorId" value={this.state.data.creatorId} onChange={this.updateValue} >
                            <option value="0" key="0"></option> 
                            {this.props.refTables.partyList.items.map(i=> {return <option value={i.id} key={i.id}>{i.record.record}</option>})}
                        </Input>   
                        
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
    form: 'PartyCreatorForm',
    fields: [],
},state => ({
    initialValues: state.form.partyCreatorForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'partyCreatorForm', data})}
)(PartyCreatorForm)



