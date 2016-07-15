/**
 * Formulář přidání nového osoby
 */

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {DropDownTree, AbstractReactComponent, i18n, Scope, Icon, FormInput} from 'components/index.jsx';
import {Modal, Button} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {refPartyNameFormTypesFetchIfNeeded} from 'actions/refTables/partyNameFormTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRegionList.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'

/**
 * ADD PARTY FORM
 * *********************************************
 * formulář nové osoby
 */
const AddPartyForm = class AddPartyForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {                                      // ve state jsou uložena a průběžně udržová data formuláře
            data : this.props.initData,                     // předvyplněná data formuláře
            errors: [],                                     // seznam chyb k vypsání uživateli
            preselect: null,
            preselectInit: true
        };
        this.bindMethods(                                   // pripojení potřebných metod - aby měly k dispozici tento objekt (formulář)
            'addComplement',                                // funkce pro přidání nového doplňku jména
            'removeComplement',                             // funkce pro odstrasnění dopňku jména
            'updateComplementValue',                        // funkce pro změnu nějaké položky v doplňku
            'updateValue',                                  // funkce pro změnu nějaké hodnoty vztahu
            'handleClose',                                  // funkce pro zavření dialogu formuláře
            'handleSubmit',                                 // funkce pro odeslání formuláře
            'validate',                                     // funkce pro kontrolu zadaných dat formuláře
            'selectRecordType',                             // výběr typu záznamu
            'dropDownTreeUpdateValue',                       // výběr typu záznamu dropdowntree
            'preselectSelects'
        );
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // načtení seznamu typů jmen
        this.dispatch(getRegistryRecordTypesIfNeeded(this.props.initData.partyTypeId));
        this.dispatch(requestScopesIfNeeded(null));
        this.preselectSelects(nextProps);
    }

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());        // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refPartyNameFormTypesFetchIfNeeded());// nacteni seznamů typů forem jmen (uřední, ...)
        this.dispatch(refPartyTypesFetchIfNeeded());        // načtení seznamu typů jmen
        this.dispatch(getRegistryRecordTypesIfNeeded(this.props.initData.partyTypeId));
        this.dispatch(requestScopesIfNeeded(null));
        this.preselectSelects(this.props);
    }

    preselectSelects(nextProps){
        var formTypeId = this.state.data.nameFormTypeId;
        if(!formTypeId && nextProps.refTables.partyNameFormTypes.fetched){
            formTypeId = nextProps.refTables.partyNameFormTypes.items[0].nameFormTypeId;
        }

        var scopeId = this.state.data.scopeId;
        if(!scopeId && nextProps.refTables.scopesData.scopes && nextProps.refTables.scopesData.scopes.length > 0
        && !nextProps.refTables.scopesData.scopes[0].isFetching){
            var scopesData = [];
            nextProps.refTables.scopesData.scopes.map(scope => {
                    if (scope.versionId === null) {
                        scopesData = scope.scopes;
                    }
                }
            );
            scopeId = scopesData[0].id;
        }


        var data = this.state.data;
        data.nameFormTypeId = formTypeId;
        data.scopeId = scopeId;
        this.setState({
            data : data                                                                     // uložení změn do state
        });
    }

    
    /**
     * UPDATE VALUE
     * *********************************************
     * aktualizace nějaké hodnoty ve formuláři
     * @params event - událost která změnu vyvolala
     */
    updateValue(event){

        var variable = event.target.name;                                                   // nazeb měněné hodnoty
        var data = this.state.data;                                                         // puvodni data formuláře

        switch(variable){
            case "recordTypeId" : data.recordTypeId = event.target.value; break;            // identifikátor typu záznamu
            case "nameFormTypeId" : data.nameFormTypeId = event.target.value; break;        // identifikátor typu jména
            case "mainPart" : data.mainPart = event.target.value; break;                    // změna hlavní části ména
            case "otherPart" : data.otherPart = event.target.value; break;                  // změna vedlejší části jména
            case "degreeBefore" : data.degreeBefore = event.target.value; break;            // změna titulu před jménem
            case "degreeAfter" : data.degreeAfter = event.target.value; break;              // změna titulu za jménem
            case "scopeId" : data.scopeId = event.target.value; break;                      // změna scope

        }
        this.setState({
            data : data                                                                     // uložení změn do state
        });
    }

    dropDownTreeUpdateValue(value){
        this.updateValue({target:{value:value, name:'recordTypeId'}});
    }


    /**
     * UPDATE COMPLEMENT VALUE
     * *********************************************
     * aktualizace nějaké hodnoty entity doplňku jména
     * @params obj complement - obsahuje index doplňku jména a měněnou hodnotu, např {index 5, variable: 'complementTypeId'} 
     * @params event - událost která změnu vyvolala
     */   
    updateComplementValue(complement, event){
        var data = this.state.data;                             // puvodní data formuláře
        for(var i=0; i<data.complements.length; i++){           // procházejí se všechny doplňky
            if(i == complement.index){                          // nalezení toho pravého, který máme změnit
                switch(complement.variable){                        
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
     * SELECT RECORD TYPE
     * *********************************************
     * vybrání typu záznamu z droptreeboxu
     */
    selectRecordType(recordTypeId){
        var data = this.state.data;                                                         // puvodni data formuláře
        data.recordTypeId = recordTypeId;                                                   // identifikátor typu záznamu
        this.setState({
            data : data                                                                     // uložení změn do state
        });
    }

    /**
     * ADD COMPLEMENT
     * *********************************************
     * přidání nového doplňku jména
     */ 
    addComplement(){
        var data = this.state.data;                         // původní data jména (formuláře)
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
    removeComplement(index, event){
        var data = this.state.data;                                 // původní data formuláře
        var complement = [];                                        // nový seznnam doplňků
        for(var i=0; i<data.complements.length; i++){               // procházejí se původní doplňky
            if(i != index){                                         // a všechny co nejsou mazaný doplněk
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
     * Kontrola vyplnění formuláře identifikátoru
     * @return array errors - seznam chyb 
     */
    validate(){
        var errors = [];                                        // seznam chyb
        var data = this.state.data;                             // zadaná data z formuláře

        //kontrola vyplnění názvu identifikátoru
        if(data.mainPart == "" || data.mainPart == null ||  data.mainPart == undefined){
            errors[errors.length] = i18n('party.errors.undefinedMainPart');
        }

        //kontrola vyplnění typu jména
        if(data.nameFormTypeId == 0 || data.nameFormTypeId == null ||  data.nameFormTypeId == undefined){
            errors[errors.length] = i18n('party.errors.undefinedNameFormType');
        }

        //kontrola vyplnění typu jména

        if(data.recordTypeId == 0 || data.recordTypeId == null ||  data.recordTypeId == undefined){
            errors[errors.length] = i18n('party.errors.undefinedRecordType');
        }

        for(var i=0; i<data.complements.length; i++){
            if(data.complements[i].complementTypeId == 0 || data.complements[i].complementTypeId == null ||  data.complements[i].complementTypeId == undefined){
                errors[errors.length] = i18n('party.name.errors.undefinedComplementType');
                break;
            }
        }

        for(var i=0; i<data.complements.length; i++){
            if(data.complements[i].complement == '' || data.complements[i].complement == null ||  data.complements[i].complement == undefined){
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
    handleClose(){
        this.dispatch(modalDialogHide());
    }

    /**
     * HANDLE SUBMIT
     * *********************************************
     * Odeslání formuláře
     */
    handleSubmit(submitType){
        var errors = this.validate();               // seznam  chyb ve vyplněných datech
        if(errors.length > 0){                      // pokud je formulář chybně vyplnění
            this.setState({             
                errors : errors                     // seznam chyb se uloží do state => dojde s přerenderování, při kterém budou chyby vypsany
            });
        }else{                                      // formulář je vyplněn dobře
            this.props.onSave(this.state.data, submitType);     // vyplněná data se pošlou do funkce definované nadřazenou komponentou v proměnné onSave 
        }
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení formuláře
     */
    render() {
        var complementsTypes = [];                                                                              // seznam aktuálních typů doplňků možných pro daný typ osoby
        for(var i=0; i<this.props.refTables.partyTypes.items.length; i++){                                      // projdu všechny typy osob co jsou
            if(this.props.refTables.partyTypes.items[i].partyTypeId == this.props.initData.partyTypeId){        // a z té která odpovídá typu této osoby
                complementsTypes = this.props.refTables.partyTypes.items[i].complementTypes;                    // si vemu seznam typů doplňků jmen
            }
        }
        var polozky = [];
        if (this.props.registryRegionRecordTypes && this.props.registryRegionRecordTypes.fetched){
            polozky = this.props.registryRegionRecordTypes.item;
        }

        return (
            <div>
                <Modal.Body>
                    <ul className="errors">
                        {this.state.errors.map(i=> {return <li>{i}</li>})}
                    </ul>
                    <form>
                        <div className="line">
                            <DropDownTree label={i18n('party.recordType')} addRegistryRecord={true} items = {polozky}  name="recordTypeId" onChange={this.dropDownTreeUpdateValue}
                                          preselect/>
                            <Scope versionId={this.props.versionId} name="scopeId" label={i18n('party.recordScope')} onChange={this.updateValue} value={this.state.data.scopeId}/>
                        </div>

                        <FormInput componentClass="select" label={i18n('party.nameFormType')} name="nameFormTypeId" onChange={this.updateValue} value={this.state.data.nameFormTypeId} >
                            <option value="0" key="0"/> 
                            {this.props.refTables.partyNameFormTypes.items.map((i, index)=> {return <option value={i.nameFormTypeId} key={i.nameFormTypeId}>{i.name}</option>})}
                        </FormInput>

                        <hr/>
                        {this.state.data.partyTypeCode == "PERSON" ?  <div className="line">
                            <FormInput type="text" label={i18n('party.degreeBefore')} name="degreeBefore" value={this.state.data.degreeBefore} onChange={this.updateValue} />
                            <FormInput type="text" label={i18n('party.degreeAfter')} name="degreeAfter" value={this.state.data.degreeAfter} onChange={this.updateValue} />
                        </div> : ""}
                        
                        <FormInput type="text" label={i18n('party.nameMain')} name="mainPart" value={this.state.data.mainPart} onChange={this.updateValue} />
                        <FormInput type="text" label={i18n('party.nameOther')} name="otherPart" value={this.state.data.otherPart} onChange={this.updateValue} />
                        <hr/>
                        <div className="line">
                            <label>{i18n('party.nameComplements')}</label>
                            {this.state.data.complements.map((j,index)=> {return <div className="block complement">
                                <div className="line">
                                    <FormInput type="text" value={j.complement} onChange={this.updateComplementValue.bind(this, {index:index, variable: 'complement'})}/>
                                    <FormInput componentClass="select" value={j.complementTypeId} onChange={this.updateComplementValue.bind(this, {index:index, variable: 'complementTypeId'})}>
                                        <option value={0} key={0}/> 
                                        {complementsTypes ? complementsTypes.map(i=> {return <option value={i.complementTypeId} key={i.complementTypeId}>{i.name}</option>}) : null}
                                    </FormInput> 
                                    <Button onClick={this.removeComplement.bind(this, index)}><Icon glyph="fa-trash"/></Button>
                                </div>
                            </div>})}
                            <Button onClick={this.addComplement}><Icon glyph="fa-plus"/></Button>
                        </div>   

                    </form>
                </Modal.Body>
                <Modal.Footer>
                    {this.props.showSubmitTypes && <Button onClick={this.handleSubmit.bind(this, 'storeAndViewDetail')}>{i18n('global.action.storeAndViewDetail')}</Button>}
                    <Button onClick={this.handleSubmit.bind(this, 'store')}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={this.handleClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

/// TODO - Chyba Redux form bez fieldů !!!!
module.exports = reduxForm({
    form: 'AddPartyForm',
    fields: [],
},state => ({
    initialValues: state.form.addPartyForm.initialValues,
    refTables: state.refTables,
        registryRegionRecordTypes: state.registryRegionRecordTypes
}),
{load: data => ({type: 'GLOBAL_INIT_FORM_DATA', form: 'addPartyForm', data})}
)(AddPartyForm)



