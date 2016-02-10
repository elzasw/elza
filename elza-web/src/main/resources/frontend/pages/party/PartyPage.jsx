/**
 * Stránka archivních pomůcek.
 */

require ('./PartyPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap'; 
import {Link, IndexLink} from 'react-router';
import {Icon, AbstractReactComponent, Ribbon, RibbonGroup, PartySearch, PartyDetail, PartyEntities, i18n} from 'components';
import {RelationForm, AddPartyForm} from 'components';
import {ButtonGroup, MenuItem, DropdownButton, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {partyDetailFetch, findPartyFetch, findPartyFetchIfNeeded} from 'actions/party/party'
import {partyAdd, insertParty, insertRelation, deleteParty} from 'actions/party/party'

/**
 * PARTY PAGE
 * *********************************************
 * Stránka osob
 */ 
var PartyPage = class PartyPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};                                // id gregoriánského kalendáře - TODO: potřeba ho dovypočíst

        this.bindMethods(                               // pripojení funkcím "this"
            'buildRibbon',                              // sestavení menu
            'handleAddParty',                           // kliknutí na tlačítko přidat osobu
            'handleDeleteParty',                        // kliknutí na tlačítko smazzat osobu
            'handleAddRelation',                        // kliknutí na tlačítko přidat ossobě vztah
            'addParty',                                 // vytvoření osoby
            'deleteParty',                              // smazání osoby
            'addRelation'                              // vytvoření relace
        );
    }

    componentDidMount(){
        this.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
    }

    componentWillReceiveProps(nextProps){
        this.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
        this.dispatch(findPartyFetchIfNeeded(nextProps.partyRegion.filterText));
    }


    /**
     * ADD PARTY
     * *********************************************
     * Uložení nové osoby
     */ 
    addParty(data) {
        this.dispatch(partyDetailFetch(data.partyId));
        this.dispatch(findPartyFetch(this.props.partyRegion.filterText));
    }

    /**
     * HANDLE ADD PARTY
     * *********************************************
     * Kliknutí na tlačítko pro založení nové osoby
     * @param partyTypeId - identifikátor typu osoby (osoba, rod, korporace, ..)
     */ 
    handleAddParty(partyTypeId) {
        this.dispatch(partyAdd(partyTypeId, null, this.addParty));
    }

    /**
     * ADD RELATION
     * *********************************************
     * Uložení nového vztahu
     * @param data - data vztahu z formuláře
     */ 
    addRelation(data) {
        var entities = [];                                                          // seznam entit vztahu
        for(var i = 0; i<data.entities.length; i++){                                // projdeme data entit z formuláře
            entities[entities.length] = {                                           // a přidáme je do seznamu nových entit
                source: data.entities[i].sources,                                   // poznámka ke vztahu o zdrojích dat
                record: {recordId: data.entities[i].record.id},
                roleType: {roleTypeId: data.entities[i].roleTypeId}                 // typ vztahu osoby a rejstříkové položky
            }
        }  
        var relation = {
            partyId: this.props.partyRegion.selectedPartyData.partyId,              // identifikátor osoby, které patří vkládaný vztah
            dateNote: data.dateNote,                                                // poznámka k dataci
            note: data.note,                                                        // poznámka
            from: data.from,                                                        // datace od
            to: data.to,                                                            // datace do
            relationEntities: entities,                                             // entity ve vztahu
            complementType:{relationTypeId:data.relationTypeId}                     // typ vztahu
        };   
        if(relation.from.textDate == "" || relation.from.textDate == null || relation.from.textDate == undefined){  
            relation.from = null;                                                   // pokud není zadaný textová část data, celý fatum se ruší
        }
        if(relation.to.textDate == "" || relation.to.textDate == null || relation.to.textDate == undefined){  
            relation.to = null;                                                   // pokud není zadaný textová část data, celý fatum se ruší
        }
        this.dispatch(insertRelation(relation, this.props.partyRegion.selectedPartyID));  //uložení vztahu a znovunačtení osoby             
    }

    /**
     * HANDLE ADD RELATION
     * *********************************************
     * Kliknutí na volnu přidání nového vztahu
     */     
    handleAddRelation(classType){
        var data = {                    
            partyTypeId: this.props.partyRegion.selectedPartyData.partyType.partyTypeId,
            partyId: this.props.partyRegion.selectedPartyID,
            note: "",
            dateNote:"",
            classType: classType,
            from: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId            
            },
            to: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId            
            },
            entities: [{
                record: null,
                roleTypeId : null,
                sources: '',
            }]
        }
        this.dispatch(modalDialogShow(this, this.props.partyRegion.selectedPartyData.record.record , <RelationForm initData={data} refTables={this.props.refTables} onSave={this.addRelation} />));
    }

    /**
     * HANDLE DELETE PARTY
     * *********************************************
     * Kliknutí na tlačítko pro smazání osoby
     */ 
    handleDeleteParty(){
        var result = confirm(i18n('party.delete.confirm')); // potvrzení smazání
        if (result) {                                       // pokud uživatel potvrdil smazání
            this.dispatch(this.deleteParty());    // smaže osobu - smazána bude aktualně vybraná osoba uložená party regionu
        }
    }

    /**
     * DELETE PARTY
     * *********************************************
     * Smazání osoby
     */ 
    deleteParty() {
        var partyId = this.props.partyRegion.selectedPartyData.partyId;                         // bude smazána aktuální osoba, uložená v partyRegionu
        return deleteParty(partyId, this.props.partyRegion.filterText);                 // smazání osoby, znovunačtení osoby i hledaných osob
    }    

    /**
     * BUILD RIBBON
     * *********************************************
     * Sestavení Ribbon Menu - přidání položek pro osoby
     */ 
    buildRibbon() {
        var isSelected = this.props.partyRegion.selectedPartyID ? true : false;
        var altActions = [];
        altActions.push(
            <DropdownButton title={<span className="dropContent"><Icon glyph='fa-download' /><div><span className="btnText">{i18n('party.addParty')}</span></div></span>}>
                {this.props.refTables.partyTypes.items.map(i=> {return <MenuItem key={i.partyTypeId} eventKey="{i.partyTypeId}" onClick={this.handleAddParty.bind(this, i.partyTypeId)}>{i.name}</MenuItem>})}
            </DropdownButton>
        );

        altActions.push(
            <Button><Icon glyph='fa-download' /><div><span className="btnText">{i18n('ribbon.action.party.import')}</span></div></Button>
        );
        var itemActions = [];
        if (isSelected) {
            itemActions.push(
                <DropdownButton title={<span className="dropContent"><Icon glyph='fa-download' /><div><span className="btnText">{i18n('party.relation.add')}</span></div></span>}>
                    {["B","E","R"].map(i =>{
                        var name = i18n('party.relation.classType.'+i).toUpperCase();
                        return <MenuItem key={"classType"+i} onClick={this.handleAddRelation.bind(this,i)}>{name}</MenuItem>
                    })}
                </DropdownButton>

                //<Button onClick={this.handleAddRelation}><Icon glyph="fa-link" /><div><span className="btnText">{i18n('party.relation.add')}</span></div></Button>
            );
            itemActions.push(
                <Button onClick={this.handleDeleteParty}><Icon glyph="fa-trash" /><div><span className="btnText">{i18n('party.delete.button')}</span></div></Button>
            );
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }
        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon party altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */ 
    render() {
        const {splitter} = this.props;

        var leftPanel = (
            <PartySearch 
                items={this.props.partyRegion.items} 
                selectedPartyID={this.props.partyRegion.selectedPartyID}
                filterText={this.props.partyRegion.filterText} 
            />
        )
        
        var centerPanel = (
            <PartyDetail 
                refTables={this.props.refTables} 
            />
        )

        var rightPanel = (
            <PartyEntities 
                partyRegion={this.props.partyRegion}
            />
        )

        return (
            <PageLayout
                splitter={splitter}
                className='party-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}


function mapStateToProps(state) {
    const {splitter, partyRegion, refTables} = state
    return {
        splitter,
        partyRegion,
        refTables
    }
}

module.exports = connect(mapStateToProps)(PartyPage);

