/**
 * Entity pro vybranou osobu
 */

require ('./partyEntities.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search, i18n} from 'components';
import {RelationForm} from 'components';
import {AppActions} from 'stores';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {updateRelation, updateParty} from 'actions/party/party'



var PartyEntities = class PartyEntities extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleUpdateRelation', 
            'handleCallUpdateRelation'
        );
    }
    
    handleCallUpdateRelation(data) {
        var relation = this.props.partyRegion.selectedPartyData.relations[0];
        relation.note="pozn255";
        relation.complementType.relationRoleTypes=1;
        this.dispatch(updateRelation(relation, this.props.partyRegion.selectedPartyData));              
    }

    handleUpdateRelation(){
        var data = {
            partyId: this.props.partyRegion.selectedPartyID,
            entities: [{
                entity: "aa",
                roleType : 1,
            }]
        }
        this.dispatch(modalDialogShow(this, this.props.partyRegion.selectedPartyData.partyId , <RelationForm initData={data} refTables={this.props.partyRegion.refTables} onSubmit={this.handleCallUpdateRelation} />));
    }

    render() {
        console.log("D");
        console.log(this.props.partyRegion.selectedPartyData);
        var entities = <div></div>;
        if(this.props.partyRegion.selectedPartyData && this.props.partyRegion.selectedPartyData.relations != null){
            entities = this.props.partyRegion.selectedPartyData.relations.map(i=> {return <div className="relation" onClick={this.handleUpdateRelation}>
                                    <strong>{i.note}</strong>
                                    <ul>
                                        {i.relationEntities==null ? '' : i.relationEntities.map(j=>{
                                            return <div className="entity">
                                                <span className="name">{j.record.record}</span>
                                                <span className="role">{j.roleType.name}</span>
                                            </div>
                                        })}
                                    </ul>
                               </div>
                    })
        };
        return  <div className="relation">
                    {entities}
                </div>
    }
}

module.exports = connect()(PartyEntities);
