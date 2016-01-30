/**
 * Komponenta detailu osoby
 */

require ('./PartyDetail.less');
require ('./PartyFormStyles.less');



import React from 'react';
import {connect} from 'react-redux'
import {Button, Input, SplitButton} from 'react-bootstrap';
import {PartyDetailNames, AbstractReactComponent, Search, i18n} from 'components';
import {AppActions} from 'stores';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {updateParty} from 'actions/party/party'
import {findPartyFetchIfNeeded} from 'actions/party/party'

var PartyDetail = class PartyDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.bindMethods(
            'handleUpdateValue',
            'handleChangeValue'
        );
        this.state={party: this.props.partyRegion.selectedPartyData};
    }
    
    componentWillReceiveProps(){
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.setState({party: this.props.partyRegion.selectedPartyData});
        console.log("NASTAVEN STATE");
        console.log(this.props.partyRegion);
    }

    handleChangeValue(e){
        console.log(this);
        var value = e.target.value;
        var variable = e.target.name;
        party = this.state.party;
        switch(variable){
            case "note" : 
                party.note = value; 
                break;
            case "history" : 
                party.history = value; 
                break;
            case "sourceInformations" : 
                party.sourceInformation = value; 
                break;
            case "from" : 
                party.from = {
                    textDate: value, 
                    calendarTypeId:1
                }; 
                break;
            case "to" : 
                party.to = {
                    textDate: value, 
                    calendarTypeId:1
                }; 
                break;
        }
        this.setState({party: party});
    }

    handleUpdateValue(e){
        var value = e.target.value;
        var variable = e.target.name;
        var party = this.state.party;   
        this.dispatch(updateParty(party));
    }

    render() {
        console.log("RENDER");
        console.log(this);
        var party = this.props.partyRegion.selectedPartyData;
        if(!party){
            return <div>Nenalezeno</div>
        }

        return <div className={"partyDetail"}>
                    <h1>{party.record.record}</h1>
                    <label>{i18n('party.detail.characteristics')}</label>
                    <p className={"characteristics"}>{party.record.characteristics}</p>

                    <div className="line">
                        <Input type="select" disabled={true} value={party.partyType.partyTypeId} label={i18n('party.detail.type')}>
                            {this.props.refTables.partyTypes.items.map(i=> {return <option value={i.partyTypeId}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('party.detail.number')} />
                    </div>
                    <div className="line">
                        <Input type="text" label={i18n('party.nameValidFrom')} name="from" value={(party.from != null ? party.from.textDate : '')} onChange={this.handleChangeValue} onBlur={this.handleUpdateValue}/>
                        <Input type="select" label={i18n('party.calendarTypeFrom')} name="calendarTypeIdFrom" value={party.from != null ? party.from.calendarTypeIdFrom : 0} onBlur={this.handleUpdateValue}>
                            <option value="0" key="0"></option> 
                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id}>{i.name}</option>})}
                        </Input>
                    </div>
 
                    <div className="line">
                        <Input type="text" label={i18n('party.nameValidTo')} name="to" value={(party.to != null ? party.to.textDate : '')} onBlur={this.handleUpdateValue}/>
                        <Input type="select" label={i18n('party.calendarTypeTo')} name="calendarTypeIdTo" value={party.to != null ? party.to.calendarTypeIdTo : 0} onBlur={this.handleUpdateValue}>
                            <option value="0" key="0"></option> 
                            {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id}>{i.name}</option>})}
                        </Input>
                    </div>                       

                    <label>{i18n('party.detail.name')}</label>
                    <PartyDetailNames data={this.props.selectedPartyData} partyRegion={this.props.partyRegion} partyId={party.partyId} />    

                    <Input type="textarea" label={i18n('party.detail.note')} name="note" value={party.note == null ? '' : party.note} onBlur={this.handleUpdateValue} />
                    <Input type="textarea" label={i18n('party.detail.history')} name="history" value={party.history == null ? '' : party.history} onBlur={this.handleUpdateValue}/>
                    <Input type="textarea" label={i18n('party.detail.sources')} name="sourceInformation" value={party.sourceInformation == null ? '' : party.history} onBlur={this.handleUpdateValue}/>
                </div>
    }
}

module.exports = connect()(PartyDetail);
