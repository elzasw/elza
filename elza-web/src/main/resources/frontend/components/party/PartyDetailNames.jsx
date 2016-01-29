/**
 * Jména zadané osoby
 */

require ('./partyEntities.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button, Glyphicon} from 'react-bootstrap';
import {AddPartyNameForm, AbstractReactComponent, Search, i18n} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {AppActions} from 'stores';
import {deleteName, updateParty} from 'actions/party/party'


var PartyDetailNames = class PartyDetailNames extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleDeleteName', 
            'deleteName',
            'handleCallAddName',
            'handleAddName',
            'handleCallEditName',
            'handleEditName'
        );
    }
    
    handleDeleteName(nameId, event){
        if(confirm(i18n('party.detail.name.delete'))){
            this.deleteName(nameId);
        }
    }

    deleteName(nameId){
        var party = this.props.partyRegion.selectedPartyData;
        var names = []
        for(var i = 0; i<party.partyNames.length; i++){
            if(i.partyNameId != nameId){
                names[names.length] = party.partyNames[i];
            }
        }
        party.partyNames = names;
        this.dispatch(updateParty(party));    
    }

    handleCallAddName(data) {
        alert("aa");
        var party = this.props.partyRegion.selectedPartyData;
        party.partyNames[party.partyNames.length] = {
            nameFormType: {
                nameFormTypeId:data.nameFormTypeId,
            },
            displayName: data.mainPart,
            mainPart: data.mainPart,
            otherPart: data.otherPart,
            degreeBefore: data.degreeBefore,
            degreeAfter: data.degreeAfter,
            validFrom: {
                    textDate:validFrom,
                    calendarTypeId:calendarTypeId
            },
            validTo: {
                    textDate:validTo,
                    calendarTypeId:calendarTypeId
            }
        }
        console.log(party);
        //this.dispatch(updateParty(party));               
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
        var party = this.props.partyRegion.selectedPartyData;
        return  <div className="partyNames">
                    <table>
                        <tbody>
                            {party.partyNames.map(i=> {return <tr className="name">
                                <th className="name column">{i.mainPart}</th> 
                                <td className="buttons">
                                    <Button className="column" onClick={this.handleEditName.bind(this, i.partyNameId)}><Glyphicon glyph="edit" /></Button>
                                    <Button className="column" onClick={this.handleDeleteName.bind(this, i.partyNameId)}><Glyphicon glyph="trash" /></Button>
                                </td>
                                <td className="description">{(i.preferred ? i18n('party.detail.name.preferred') : "" )}</td>
                            </tr>})}
                        </tbody>
                    </table>
                    <Button className="column" onClick={this.handleAddName}><Glyphicon glyph="plus" /> { i18n('party.detail.name.new')}</Button>
                </div>
    }
}

module.exports = connect()(PartyDetailNames);
