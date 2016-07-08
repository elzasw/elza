/**
 * Formulář autora osoby
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, i18n, Autocomplete} from 'components/index.jsx';
import {Modal, Button, Input, Glyphicon} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refPartyListFetchIfNeeded} from 'actions/refTables/partyList.jsx'

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
            errors: [],                                      // sezn chyb k vypsání uživateli
            partyList: []
        };
        this.bindMethods(                                   // pripojení potřebných metod - aby měly k dispozici tento objekt (formulář)
            'updateValue',                                  // funkce pro změnu nějaké hodnoty identifikátoru
            'validate',                                     // funkce pro kontrolu zadaných dat formuláře
            'handleClose',                                  // funkce pro zavření okna dialogu
            'handleSubmit',                                  // funkce pro odeslání formuláře
            'handleSearchChange'
        );

    }

    /**
     * UPDATE VALUE
     * *********************************************
     * aktualizace nějaké hodnoty ve formuláři
     * @params event - událost která změnu vyvolala
     */
    updateValue(id, valueObj){
        var value = id;                                               // hodnota změněného pole formuláře
        var data = this.state.data;
        data.creatorId = value;
        data.creatorName = valueObj.name;

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

    handleSearchChange(text) {

        text = text == "" ? null : text;

        WebApi.findPartyForParty(this.props.partyId, text)
                .then(json => {
                    this.setState({
                        partyList: json.map(party => {
                            return {
                                id: party.partyId,
                                name: party.record.record,
                                type: party.partyType.name,
                                from: party.from,
                                to: party.to,
                                characteristics: party.record.characteristics
                            }
                        })
                    })
                })
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

                        <Autocomplete
                                label={i18n('party.creator.creator')}
                                customFilter
                                className='autocomplete-party'
                                items={this.state.partyList}
                                getItemId={(item) => item ? item.id : null}
                                getItemName={(item) => item ? item.name : ''}
                                onSearchChange={this.handleSearchChange}
                                onChange={this.updateValue}
                                renderItem={this.props.renderParty}
                                 />

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



