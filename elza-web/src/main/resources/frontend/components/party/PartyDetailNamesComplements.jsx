
/**
 * Doplnky jména
 */

require ('./partyEntities.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button, Glyphicon} from 'react-bootstrap';
import {AddPartyNameForm, AbstractReactComponent, Search, i18n} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {AppActions} from 'stores';
import {deleteName, updateParty} from 'actions/party/party'


var PartyDetailNamesComplements = class PartyDetailNamesComplements extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleDeleteComplement', 
            'deleteComplement',
            'handleCallAddComeplement',
            'handleAddComplement',
            'handleCallEditComplement',
            'handleEditComplement'
        );
    }
    
    handleDeleteComplement(index){
        if(confirm(i18n('party.detail.complement.delete'))){
            this.deleteComplement(index);
        }
    }

    deleteComplement(index){
        var complements = this.state.complements;
        var newComplements = []
        for(var i = 0; i<complements.length; i++){
            if(i != index){
                newComplements[newComplements.length] = complements[i];
            }
        }
        this.setState({
            complements : complements
        });
        this.dispatch(updateParty(party));    
    }

    handleCallAddName(data) {
        var party = this.props.partyRegion.selectedPartyData;
        party.partyNames[party.partyNames.length] = {
            nameFormTypeId: data.nameFormTypeId,
            mainPart: data.mainPart,
            otherPart: data.otherPart,
            degreeBefore: data.degreeBefore,
            degreeAfter: data.degreeAfter
        }
        this.dispatch(updateParty(party));               
    }

    handleAddName(){
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.new') , <AddPartyNameForm onSubmit={this.handleCallAddName} />));
    }

    handleEditName(nameId){
        var party = this.props.partyRegion.selectedPartyData;
        var data = {};
        for(var i = 0; i<party.partyNames.length; i++){
            if(party.partyNames[i].partyNameId == nameId){
                data = party.partyNames[i];
            }
        }
        console.log(data);
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.new') , <AddPartyNameForm initData={data} onSubmit={this.handleCallEditName} />));
    }

    handleCallEditName(data){
        var party = this.props.partyRegion.selectedPartyData;
        names = [];
        this.dispatch(updateParty(party));         
    }

    render() {
        console.log(this.props.data);
        return  <div className="partyNames">
                    <table>
                        <tbody>
                            {this.props.data.partyNames.map(i=> {return <tr className="name">
                                <th className="name column">{i.mainPart}</th> 
                                <td className="buttons">
                                    <Button className="column" onClick={this.handleEditName.bind(this, i.partyNameId)}><Glyphicon glyph="edit" /></Button>
                                    <Button className="column" onClick={this.handleDeleteName.bind(this, i.partyNameId)}><Glyphicon glyph="trash" /></Button>
                                </td>
                                <td className="description">{(i.partyNameId == this.props.data.preferredName.partyNameId ? i18n('party.detail.name.preferred') : "" )}</td>
                            </tr>})}
                        </tbody>
                    </table>
                    <Button className="column" onClick={this.handleAddName}><Glyphicon glyph="plus" /> { i18n('party.detail.name.new')}</Button>
                </div>
    }
}

module.exports = connect()(PartyDetailNamesComplements);
