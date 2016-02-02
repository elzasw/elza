/**
 * Entity pro vybranou osobu
 */

require ('./partyEntities.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button, Glyphicon} from 'react-bootstrap';
import {RelationForm, AbstractReactComponent, i18n} from 'components'
import {AppActions} from 'stores';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {updateRelation, deleteRelation} from 'actions/party/party'


/**
* PARTY ENTITIES
* *********************************************
* panel pro správu entit jedné osoby
*/ 
var PartyEntities = class PartyEntities extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'updateRelation', 
            'deleteRelation', 
            'handleUpdateRelation',
            'handleDeleteRelation', 
            'handleDeleteRelation'
        );
    }
    
    /**
     * UPDATE RELATION
     * *********************************************
     * Uložení změn v jednom vztahu po odeslání formuláře se změnani
     * @param data - data vyplněná v formuláři
     */ 
    updateRelation(data) {
        var partyId = this.props.partyRegion.selectedPartyData.partyId;             // identifikátor osoby, které patří měněný vztah
        var relations = this.props.partyRegion.selectedPartyData.relations;         // původní vztahy osoby
        var relation = null;                                                        // původní verze relace, kterou budeme měnit
        for(var i = 0; i<relations.length; i++){                                    // je potřeba jí najít mezi ostatními relacemi
            if(relations[i].relationId == data.relationId){                         // to je ona            
                relation = relations[i];                                            // zapamatujeme si, která to je
            }
        }
        var entities = [];                                                          // nový (zatím prázdný) seznam entit vztahu
        for(var i = 0; i<data.entities.length; i++){                                // projdeme data entit z formuláře
            entities[entities.length] = {                                           // a přidáme je do seznamu nových entit
                source: data.entities[i].sources,                                   // poznámka ke vztahu o zdrojích dat
                record: {recordId: data.entities[i].recordId},                      // rejstříková položka
                roleType: {roleTypeId: data.entities[i].roleTypeId},                // typ vztahu osoby a rejstříkové položky
                relationEntityId: data.entities[i].relationEntityId                 // identifikátor entity vztahu
            }
        }        
        relation.dateNote = data.dateNote;                                          // poznámka k dataci
        relation.note = data.note;                                                  // poznámka ke vztahu
        relation.from = data.from;                                                  // datace vztahu od
        relation.to = data.to;                                                      // datace vztahu do
        relation.relationEntities = entities;                                       // seznamm entit ve vztahu
        relation.complementType.relationTypeId = data.relationTypeId;               // typ vztahu
        if(
            !relation.from ||
            relation.from.textDate == "" || 
            relation.from.textDate == null || 
            relation.from.textDate == undefined
        ){  
            relation.from = null;                                                   // pokud není zadaný textová část data, celý fatum se ruší
        }
        if(
            !relation.to || 
            relation.to.textDate == "" || 
            relation.to.textDate == null || 
            relation.to.textDate == undefined
        ){  
            relation.to = null;                                                     // pokud není zadaný textová část data, celý fatum se ruší
        }

        this.dispatch(updateRelation(relation,partyId));                            // uložení změn a znovu načtení dat osoby              
    }

    /**
     * HANDLE UPDATE RELATION
     * *********************************************
     * Kliknutí na ikonu editace vztahu
     */ 
    handleUpdateRelation(relationId){
        var party = this.props.partyRegion.selectedPartyData;                       // osoba, které paří vztah, co bysme chtěli měnit
        var relation = {};                                                          // proměná pro relaci, kterou budeme měnit
        for(var i = 0; i<party.relations.length; i++){                              // musíme jí najit mezi ostatníma relacena
            if(party.relations[i].relationId == relationId){                        // to je ona
                relation = party.relations[i];                                      // uložíme si ji do připravené proměnné
            }
        };
        var entities = [];                                                          // seznam entit ve vztahu
        if(relation.relationEntities){                                              // pokud má vztah nějakké entity
            for(var i = 0; i<relation.relationEntities.length; i++){                // tak se projdou
                entities[entities.length]={                                         // a přidají so seznamu vztahů
                    recordId: relation.relationEntities[i].record.recordId,         // identifikátor rejstříkového hesla
                    roleTypeId : relation.relationEntities[i].roleType.roleTypeId,  // typ role entity ve vztahu
                    sources: relation.relationEntities[i].source,                   // poznámka ke zdrojům dat
                    relationEntityId: relation.relationEntities[i].relationEntityId // identifikátor entity vztahu
                };
            };
        }
        var data = {                                                                // data, která odešleme formuláři pro editace
            partyTypeId: party.partyType.partyTypeId,                               // typ osoby - podle typu osoby budou pak na výběr specifické typy vztahů
            relationId: relation.relationId,                                        // identifikátor vztahu
            relationTypeId: relation.complementType.relationTypeId,                 // typ vztahu
            note : relation.note,                                                   // poznnámka ke vttahu
            dateNote: relation.dateNote,                                            // poznámka k dataci vztahu
            from: {                                                                 // datace od
                textDate: (relation.from != null ? relation.from.textDate : ""),    
                calendarTypeId: (relation.from != null ? relation.from.calendarTypeId : this.props.partyRegion.gregorianCalendarId )
            },
            to: {                                                                   // datace do
                textDate: (relation.to != null ? relation.to.textDate : ""), 
                calendarTypeId: (relation.to != null ? relation.to.calendarTypeId : this.props.partyRegion.gregorianCalendarId)
            },
            entities: entities,                                                     // entity ve vztahu
        }
        this.dispatch(                                                              // otevření formáláře s editací vztahu
            modalDialogShow(this, this.props.partyRegion.selectedPartyData.record.record , 
            <RelationForm initData={data} refTables={this.props.partyRegion.refTables} onSave={this.updateRelation} />)
        );
    }

    /**
     * HANDLE DELETE RELATION
     * *********************************************
     * Kliknutí na ikonu smazání vztahu
     * @param int relationId - identifikátor vztahu, který chceme smazzat
     * @param event - událost, která provází kliknnutí na smazat
     */ 
    handleDeleteRelation(relationId, event){
        if(confirm(i18n('party.relation.delete.confirm'))){         // pokud uživatel potvrdí takové okénko že chce vztah opravdu smazat
            this.deleteRelation(relationId);                        // zavolá se funkce, která ho smaže
        }
    }   

    /**
     * DELETE RELATION
     * *********************************************
     * Smazání vztahu
     * @param int relationId - identifikátor vztahu, který chceme smazzat
     */ 
    deleteRelation(relationId){
        var partyId = this.props.partyRegion.selectedPartyData.partyId;         // identifikátor osoby, které chceme vztah smazat
        this.dispatch(deleteRelation(relationId, partyId));                     // smazání vztahu a znovunačtení dat osoby
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení panelu vztahů
     */ 
    render() {
        var entities = <div></div>;
        if(this.props.partyRegion.selectedPartyData && this.props.partyRegion.selectedPartyData.relations != null){
            entities = this.props.partyRegion.selectedPartyData.relations.map(i=> {
                return <div className="relation-entity">
                            <strong>{i.note}</strong>
                                {i.relationEntities==null ? '' : i.relationEntities.map(j=>{
                                    return <div className="entity">
                                        <div className="name">{j.record.record}</div>
                                        <div className="role">{j.roleType.name}</div>
                                    </div>
                                })}
                            <Button className="column" onClick={this.handleUpdateRelation.bind(this, i.relationId)}><Glyphicon glyph="edit" /></Button>  
                            <Button className="column" onClick={this.handleDeleteRelation.bind(this, i.relationId)}><Glyphicon glyph="trash" /></Button>
                        </div>
                })
        };
        return  <div className="relations">
                    {entities}
                </div>
    }
}

module.exports = connect()(PartyEntities);
