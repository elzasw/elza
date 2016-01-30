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



var PartyEntities = class PartyEntities extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleUpdateRelation', 
            'handleCallUpdateRelation',
            'handleDeleteRelation'/*,
            'handleCallDeleteRelation'*/
        );
    }
    
    handleCallUpdateRelation(data) {
        console.log("data");
        console.log(data);
        var relations = this.props.partyRegion.selectedPartyData.relations;
        var relation = null;
        for(var i = 0; i<relations.length; i++){
            if(relations[i].relationId == data.relationId){
                relation = relations[i];
            }
        }
        var entities = [];
        for(var i = 0; i<data.entities.length; i++){
            entities[entities.length] = {
                source: data.entities[i].sources,
                record: {recordId: data.entities[i].recordId},
                roleType: {roleTypeId: data.entities[i].roleTypeId}
            }
        }        
        
        relation.dateNote = data.dateNote;
        relation.note = data.note;
        relation.from = {
                textDate: data.dateFrom,
                calendarTypeId: data.calendarTypeIdFrom
        };
        relation.to = {
                textDate: data.dateTo,
                calendarTypeId: data.calendarTypeIdTo
        };
        relation.relationEntities = entities;
        relation.complementType.relationTypeId = data.relationTypeId;
        this.dispatch(updateRelation(relation, this.props.partyRegion.selectedPartyId));              
    }

    handleUpdateRelation(relationId){
        var party = this.props.partyRegion.selectedPartyData;
        var relation = {};
        for(var i = 0; i<party.relations.length; i++){
            if(party.relations[i].relationId == relationId){
                relation = party.relations[i];
            }
        };
        var entities = [];
        for(var i = 0; i<relation.relationEntities.length; i++){
            entities[entities.length]={
                registerId: relation.relationEntities[i].record.externalId,
                roleTypeId : relation.relationEntities[i].roleType.roleTypeId,
                sources: relation.relationEntities[i].source,           
            };
        };       
        var data = {
            partyTypeId: party.partyType.partyTypeId,
            relationId: relation.relationId,
            relationTypeId: relation.complementType.relationTypeId,
            note : relation.note,
            dateNote: relation.dateNote,
            dateFrom: (relation.from != null ? relation.from.textDate : ""), 
            dateTo: (relation.to != null ? relation.to.textDate : ""),
            calendarTypeIdFrom: (relation.from != null ? relation.from.calendarTypeId : 0),
            calendarTypeIdTo: (relation.to != null ? relation.to.calendarTypeId : 0),
            entities: entities,
        }
        this.dispatch(modalDialogShow(this, this.props.partyRegion.selectedPartyData.record.record , <RelationForm initData={data} refTables={this.props.partyRegion.refTables} onSubmit={this.handleCallUpdateRelation} />));
    }

    handleDeleteRelation(relationId, event){
        if(confirm(i18n('party.relation.delete.confirm'))){
            this.deleteRelation(relationId);
        }
    }

    deleteRelation(relationId){
        var partyId = this.props.partyRegion.selectedPartyData.partyId;
        this.dispatch(deleteRelation(relationId, partyId));    
    }

    render() {
        var entities = <div></div>;
        if(this.props.partyRegion.selectedPartyData && this.props.partyRegion.selectedPartyData.relations != null){
            entities = this.props.partyRegion.selectedPartyData.relations.map(i=> {return <div className="relation">
                                    <strong>{i.note}</strong>
                                    <ul>
                                        {i.relationEntities==null ? '' : i.relationEntities.map(j=>{
                                            return <div className="entity">
                                                <span className="name">{j.record.record}</span>
                                                <span className="role">{j.roleType.name}</span>
                                            </div>
                                        })}
                                    </ul>
                                    <Button className="column" onClick={this.handleUpdateRelation.bind(this, i.relationId)}><Glyphicon glyph="edit" /></Button>  
                                    <Button className="column" onClick={this.handleDeleteRelation.bind(this, i.relationId)}><Glyphicon glyph="trash" /></Button>       
                               </div>
                    })
        };
        return  <div className="relation">
                    {entities}
                </div>
    }
}

module.exports = connect()(PartyEntities);
