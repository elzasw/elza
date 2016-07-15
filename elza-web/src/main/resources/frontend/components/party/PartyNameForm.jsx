/**
 * Formulář přidání nového jména osobě
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField} from 'components/form/FormUtils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'


/**
 * PARTY NAME FORM
 * *********************************************
 * Formulář jména osoby
 */
const PartyNameForm = class PartyNameForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {                                      // ve state jsou uložena a průběžně udržová data formuláře
            data : this.props.initData,                     // předvyplněná data formuláře
            errors: []                                      // sezn chyb k vypsání uživateli
        };
        this.bindMethods(                                   // pripojení potřebných metod - aby měly k dispozici tento objekt (formulář)
            'addComplement',                                // funkce pro přidání nového doplňku jména
            'removeComplement',                             // funkce pro odstrasnění dopňku jména
            'updateComplementValue',                        // funkce pro změnu nějaké položky v doplňku
            'updateValue',                                  // funkce pro změnu nějaké hodnoty vztahu
            'handleClose',                                  // funkce pro zavření dialogu formuláře
            'handleSubmit',                                 // funkce pro odeslání formuláře
            'validate'                                      // funkce pro kontrolu zadaných dat formuláře
        );
    }

    componentDidMount() {
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// seznam typů jmén (uřední, ...)
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // budeme potřebovat také seznam typů osob (osoba, rod, korporace, ..)
    }

    componentWillReceiveProps() {
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// seznam typů jmén (uřední, ...)
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // budeme potřebovat také seznam typů osob (osoba, rod, korporace, ..)
    }

    /**
     * UPDATE VALUE
     * *********************************************
     * aktualizace nějaké hodnoty ve formuláři (kromě doplňků jmen)
     * @params event - událost která změnu vyvolala
     */
    updateValue(event) {
        const value = event.target.value;                                                     // hodnota změněného pole formuláře
        const variable = event.target.name;                                                   // nazeb měněné hodnoty
        const data = this.state.data;                                                         // puvodni data formuláře
        switch(variable) {
            case "nameFormTypeId" : data.nameFormTypeId = value; break;        // změna typu jména
            case "mainPart" : data.mainPart = value; break;                    // změna hlavní části jména
            case "otherPart" : data.otherPart = value; break;                  // změna vedlejší části jména
            case "degreeAfter" : data.degreeAfter = value; break;
            case "degreeBefore" : data.degreeBefore = value; break;
            case "fromText" : data.validFrom.textDate = value; break;          // změna data od
            case "toText" : data.validTo.textDate = value; break;              // změna data do
            case "fromCalendar" : data.validFrom.calendarTypeId = value; break;// změna typu kalendáře od
            case "toCalendar" : data.validTo.calendarTypeId = value; break;    // změna typu kalendáře do
        }
        this.setState({
            data : data                                                                     // uložení změn do state
        });
    }

    /**
     * UPDATE COMPLEMENT VALUE
     * *********************************************
     * aktualizace nějaké hodnoty entity doplňku jména
     * @params obj complement - obsahuje index doplňku jména a měněnou hodnotu, např {index 5, variable: 'complementTypeId'} 
     * @params event - událost která změnu vyvolala
     */   
    updateComplementValue(complement, event) {
        const data = this.state.data;                             // puvodní data formuláře
        for(let i=0; i<data.complements.length; i++) {           // procházejí se všechny doplňky
            if(i == complement.index) {                          // nalezení toho pravého, který máme změnit
                switch(complement.variable) {                        
                    case "complementTypeId" : data.complements[complement.index].complementTypeId = event.target.value; break;
                    case "complement" : data.complements[complement.index].complement = event.target.value; break;
                }
            }
        }
        this.setState({
            data : data                                         // uložení změny do state
        });
    }


    /**
     * ADD COMPLEMENT
     * *********************************************
     * přidání nového doplňku jména
     */ 
    addComplement() {
        const data = this.state.data;                         // původní data jména (formuláře)
        data.complements[data.complements.length]={         // pridání nového prázdného doplňku na konec seznamu jmen
            complementTypeId: null,
            complement: null
        }
        this.setState({
            data : data                                     // uložení výsledku do state
        });
    }

    /**
     * REMOVE COMPLEMENT
     * *********************************************
     * odstranění jednoho doplňku jména
     * @params int index - lokální index doplňku, který se má odstranit 
     * @params event - událost která změnu vyvolala
     */ 
    removeComplement(index, event) {
        const data = this.state.data;                                 // původní data formuláře
        const complement = [];                                        // nový seznnam doplňků
        for(let i=0; i<data.complements.length; i++) {               // procházejí se původní doplňky
            if(i != index) {                                         // a všechny co nejsou mazaný doplněk
               complement[complement.length] = data.complements[i]; // přidáme do nových doplňků
            }
        }
        data.complements = complement;                              // stare doplňky v datech vymeníme za nové
        this.setState({
            data : data                                             // a uložíme nová data do state
        });
    }

    /**
     * VALIDATE
     * *********************************************
     * Kontrola vyplnění formuláře jména
     * @return array errors - seznam chyb 
     */
    validate() {
        const errors = [];                                        // seznam chyb
        const data = this.state.data;                             // zadaná data z formuláře

        //kontrola vyplnění typu jména
        if(data.nameFormTypeId == 0 || data.nameFormTypeId == null ||  data.nameFormTypeId == undefined) {
            errors[errors.length] = i18n('party.name.errors.undefinedNameFormType');
        }

        //kontrola vyplnění hlavního jména
        if(data.mainPart == "" || data.mainPart == null ||  data.mainPart == undefined) {
            errors[errors.length] = i18n('party.name.errors.undefinedMainPart');
        }

        for(let i=0; i<data.complements.length; i++) {
            if(data.complements[i].complementTypeId == 0 || data.complements[i].complementTypeId == null ||  data.complements[i].complementTypeId == undefined) {
                errors[errors.length] = i18n('party.name.errors.undefinedComplementType');
                break;
            }
        }

        for(let i=0; i<data.complements.length; i++) {
            if(data.complements[i].complement == '' || data.complements[i].complement == null ||  data.complements[i].complement == undefined) {
                errors[errors.length] = i18n('party.name.errors.undefinedComplementValue');
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
        let complementsTypes = [];                                                                              // seznam aktuálních typů doplňků možných pro daný typ osoby
        for(let i=0; i<this.props.refTables.partyTypes.items.length; i++) {                                      // projdu všechny typy osob co jsou
            if(this.props.refTables.partyTypes.items[i].partyTypeId == this.props.initData.partyTypeId) {        // a z té která odpovídá typu této osoby
                complementsTypes = this.props.refTables.partyTypes.items[i].complementTypes;                    // si vemu seznam typů doplňků jmen
            }
        }
        return (
            <div>
                <Modal.Body>
                    <ul className="errors">
                        {this.state.errors.map((i, index)=> {return <li key={index}>{i}</li>})}
                    </ul>
                    <form>
                        <FormInput componentClass="select" label={i18n('party.nameFormType')} name="nameFormTypeId" value={this.state.data.nameFormTypeId} onChange={this.updateValue}>
                            <option value="0" key="0"/> 
                            {this.props.refTables.partyNameFormTypes.items.map(i=> {return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>})}
                        </FormInput>

                        {this.state.data.partyTypeCode == "PERSON" ?
                         <div className="line">
                            <FormInput type="text" label={i18n('party.degreeBefore')} name="degreeBefore" value={this.state.data.degreeBefore} onChange={this.updateValue} />
                            <FormInput type="text" label={i18n('party.degreeAfter')} name="degreeAfter" value={this.state.data.degreeAfter} onChange={this.updateValue} />
                        </div> : "" }

                        <FormInput type="text" label={i18n('party.nameMain')} name="mainPart" value={this.state.data.mainPart} onChange={this.updateValue} />
                        <FormInput type="text" label={i18n('party.nameOther')} name="otherPart" value={this.state.data.otherPart} onChange={this.updateValue} />
                        <div className="line datation">
                            <div className="date line">
                                <div>
                                    <label>{i18n('party.name.from')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" name="fromCalendar" value={this.state.data.validFrom.calendarTypeId} onChange={this.updateValue} >
                                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                        </FormInput>
                                        <FormInput type="text"  name="fromText" value={this.state.data.validFrom.textDate} onChange={this.updateValue} />
                                    </div>
                                </div>
                                <div>
                                    <label>{i18n('party.name.to')}</label>
                                    <div className="line">
                                        <FormInput componentClass="select" name="toCalendar" value={this.state.data.validTo.calendarTypeId} onChange={this.updateValue} >
                                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                        </FormInput>
                                        <FormInput type="text" name="toText" value={this.state.data.validTo.textDate} onChange={this.updateValue} />
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="line">
                            <label>{i18n('party.nameComplements')}</label>
                            {this.state.data.complements.map((j,index)=> {return <div className="block complement">
                                <div className="line">
                                    <FormInput componentClass="select" value={j.complementTypeId} onChange={this.updateComplementValue.bind(this, {index:index, variable: 'complementTypeId'})}>
                                        <option value={0} key={0}/>
                                        {complementsTypes ? complementsTypes.map(i=> {return <option value={i.complementTypeId} key={i.complementTypeId}>{i.name}</option>}) : null}
                                    </FormInput>
                                    <FormInput type="text" value={j.complement} onChange={this.updateComplementValue.bind(this, {index:index, variable: 'complement'})}/>
                                    <Button onClick={this.removeComplement.bind(this, index)}><Icon glyph="fa-trash"/></Button>
                                </div>
                            </div>})}
                            <Button onClick={this.addComplement}><Icon glyph="fa-plus"/></Button>
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
    form: 'PartyNameForm',
    fields: [],
},state => ({
    initialValues: state.form.partyNameForm.initialValues,
    refTables: state.refTables
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'partyNameForm', data})}
)(PartyNameForm)



