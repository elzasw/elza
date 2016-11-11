import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {RelationForm, AbstractReactComponent, i18n, Icon} from 'components/index.jsx';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {updateRelation, deleteRelation} from 'actions/party/party.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import './PartyEntities.less';

/**
 * Entity pro vybranou osobu
 */
//TODO @compel
const PartyEntities = class PartyEntities extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'updateRelation', 
            'deleteRelation', 
            'handleUpdateRelation',
            'handleDeleteRelation', 
            'handleDeleteRelation',
            'trySetFocus'
        );
    }

    componentDidMount() {
        this.trySetFocus()
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    trySetFocus(props = this.props) {
        const {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'party', 3) || isFocusFor(focus, 'party', 3, 'list')) {
                this.setState({}, () => {
                    this.refs.relationsList.focus()
                    focusWasSet()
                })
            }
        }
    }

    /**
     * UPDATE RELATION
     * *********************************************
     * Uložení změn v jednom vztahu po odeslání formuláře se změnani
     * @param data - data vyplněná v formuláři
     */ 
    updateRelation(data) {
        const {partyRegion:{selectedPartyData:{partyId, relations}}} = this.props;  // identifikátor osoby, které patří měněný vztah a původní vztahy osoby
        let relation = null;                                                        // původní verze relace, kterou budeme měnit
        for(let i = 0; i<relations.length; i++){                                    // je potřeba jí najít mezi ostatními relacemi
            if(relations[i].relationId == data.relationId){                         // to je ona            
                relation = relations[i];                                            // zapamatujeme si, která to je
            }
        }
        var entities = [];                                                          // nový (zatím prázdný) seznam entit vztahu
        for(let i = 0; i<data.entities.length; i++){                                // projdeme data entit z formuláře
            entities[entities.length] = {                                           // a přidáme je do seznamu nových entit
                source: data.entities[i].sources,                                   // poznámka ke vztahu o zdrojích dat
                record: {
                    "@class": ".RegRecordVO",
                    id: data.entities[i].record.id
                },                                    // rejstříková položka
                roleType: {roleTypeId: data.entities[i].roleTypeId},                // typ vztahu osoby a rejstříkové položky
                relationEntityId: data.entities[i].relationEntityId                 // identifikátor entity vztahu
            }
        }
        relation = {
            ...relation,
            dateNote: data.dateNote,                                          // poznámka k dataci
            note: data.note,                                                  // poznámka ke vztahu
            from: data.from,                                                  // datace vztahu od
            to: data.to,
            source: data.source,
            relationEntities: entities,                                       // seznamm entit ve vztahu
            complementType: {
                ...relation.complementType,
                relationTypeId:data.relationTypeId                              // typ vztahu
            },
        };

        if(
            !relation.from ||
            relation.from.textDate == "" || 
            relation.from.textDate == null || 
            relation.from.textDate == undefined
        ) {
            relation.from = null;                                                   // pokud není zadaný textová část data, celý datum se ruší
        }
        if(
            !relation.to || 
            relation.to.textDate == "" || 
            relation.to.textDate == null || 
            relation.to.textDate == undefined
        ) {
            relation.to = null;                                                     // pokud není zadaný textová část data, celý datum se ruší
        }

        this.dispatch(updateRelation(relation,partyId));                            // uložení změn a znovu načtení dat osoby              
    }

    /**
     * HANDLE UPDATE RELATION
     * *********************************************
     * Kliknutí na ikonu editace vztahu
     */ 
    handleUpdateRelation(relationId){
        const {partyRegion: {selectedPartyData, gregorianCalendarId, refTables}} = this.props;
        const party = selectedPartyData;                                             // osoba, které paří vztah, co bysme chtěli měnit
        let relation = {};                                                          // proměná pro relaci, kterou budeme měnit
        for(let i = 0; i<party.relations.length; i++){                              // musíme jí najit mezi ostatníma relacena
            if(party.relations[i].relationId == relationId){                        // to je ona
                relation = party.relations[i];                                      // uložíme si ji do připravené proměnné
            }
        };
        let entities = [];                                                          // seznam entit ve vztahu
        if(relation.relationEntities){                                              // pokud má vztah nějakké entity
            for(let i = 0; i<relation.relationEntities.length; i++){                // tak se projdou
                entities[entities.length]={                                         // a přidají so seznamu vztahů
                    record: {
                        id: relation.relationEntities[i].record.id,
                        record: relation.relationEntities[i].record.record,
                        characteristics: relation.relationEntities[i].record.characteristics
                    },
                    roleTypeId : relation.relationEntities[i].roleType.roleTypeId,  // typ role entity ve vztahu
                    sources: relation.relationEntities[i].source,                   // poznámka ke zdrojům dat
                    relationEntityId: relation.relationEntities[i].relationEntityId // identifikátor entity vztahu
                };
            };
        }
        const data = {
            partyId: party.partyId,                                                 // data, která odešleme formuláři pro editace
            partyTypeId: party.partyType.partyTypeId,                               // typ osoby - podle typu osoby budou pak na výběr specifické typy vztahů
            relationId: relation.relationId,                                        // identifikátor vztahu
            classType: relation.complementType.classType,
            relationTypeId: relation.complementType.relationTypeId,                 // typ vztahu
            note : relation.note,                                                   // poznnámka ke vttahu
            dateNote: relation.dateNote,                                            // poznámka k dataci vztahu
            source: relation.source,
            from: {                                                                 // datace od
                textDate: (relation.from != null ? relation.from.textDate : ""),    
                calendarTypeId: (relation.from != null ? relation.from.calendarTypeId : gregorianCalendarId )
            },
            to: {                                                                   // datace do
                textDate: (relation.to != null ? relation.to.textDate : ""), 
                calendarTypeId: (relation.to != null ? relation.to.calendarTypeId : gregorianCalendarId)
            },
            entities: entities,                                                     // entity ve vztahu
        }
        this.dispatch(                                                              // otevření formáláře s editací vztahu
            modalDialogShow(this, selectedPartyData.record.record ,
            <RelationForm initData={data} refTables={refTables} onSave={this.updateRelation} />)
        );
    }

    /**
     * HANDLE DELETE RELATION
     * *********************************************
     * Kliknutí na ikonu smazání vztahu
     * @param relationId int - identifikátor vztahu, který chceme smazzat
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
     * @param relationId int - identifikátor vztahu, který chceme smazzat
     */ 
    deleteRelation(relationId){
        var partyId = this.props.partyRegion.selectedPartyData.partyId;         // identifikátor osoby, které chceme vztah smazat
        this.dispatch(deleteRelation(relationId, partyId));                     // smazání vztahu a znovunačtení dat osoby
    }

    /**
     * Převede datum na text.
     * @param dateVo datum
     * @returns datum jako text
     */
    formatDateToText(dateVo) {
        if(dateVo == undefined){
            return "";
        }

        var result;
        if (dateVo.format && dateVo.format.indexOf("-") > -1) {
            result = "[" + dateVo.textDate + "]";
        } else {
            result = dateVo.textDate;
        }
        return result;
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení panelu vztahů
     */ 
    render() {
        const {canEdit, partyRegion: {selectedPartyData}} = this.props;
        
        let entities;

        if(selectedPartyData && selectedPartyData.relations != null){
            entities = selectedPartyData.relations.map(i=> {
                let icon;
                switch (i.complementType.classType){
                    case "B": icon = "fa-plus"; break;
                    case "E": icon = "fa-minus"; break;
                    case "R": icon = "fa-code-fork"; break;
                }

                const relationEntities = i.relationEntities ? i.relationEntities.map(e =>e.record.record).join(", ") : null;

                let date = this.formatDateToText(i.from);
                date += i.from != undefined && i.to != undefined  ? " - " + this.formatDateToText(i.to) : this.formatDateToText(i.to);

                return <div className="relation-entity" key={"relation-" + i.relationId}>
                            <div className="block-row title"><Icon glyph={icon}/> <strong>{i.complementType.name}</strong></div>

                            <div className="block-row">{relationEntities}</div>

                            {date ? <div className="block-row">{date}</div> : ""}

                            <div className="block-row actions">
                                {canEdit && <Button className="column" onClick={this.handleUpdateRelation.bind(this, i.relationId)}><Icon glyph="fa-pencil"/></Button>}
                                {canEdit && <Button className="column" onClick={this.handleDeleteRelation.bind(this, i.relationId)}><Icon glyph="fa-trash"/></Button>}
                            </div>
                       </div>
                })
        };
        return  <div className="relations" ref='relationsList' tabIndex={0}>
                    {entities}
                </div>
    }
}

function mapStateToProps(state) {
    const {focus, userDetail} = state
    return {
        focus,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(PartyEntities);
