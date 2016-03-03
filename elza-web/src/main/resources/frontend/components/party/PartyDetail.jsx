/**
 * Komponenta detailu osoby
 */

require ('./PartyFormStyles.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button, Input, SplitButton} from 'react-bootstrap';
import {PartyDetailCreators, PartyDetailIdentifiers, PartyDetailNames, AbstractReactComponent, Search, i18n} from 'components';
import {AppActions} from 'stores';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {updateParty} from 'actions/party/party'
import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party'
import {Utils} from 'components'
import {setInputFocus} from 'components/Utils'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var keyModifier = Utils.getKeyModifier()

var keymap = {
    PartyDetail: {
        xxx: keyModifier + 'e',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

/**
 * PARTY DETAIL
 * *********************************************
 * Detail osoby
 */ 
var PartyDetail = class PartyDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(                               // pripojením funkcím "this"
            'updateValue',                              // aktualizace nějaké hodnoty
            'changeValue',                               // změna nějakéh políčka ve formuláři
            'trySetFocus'
        );
        this.state={
            dynastyId: 2,
            groupId: 3,
        };
    }

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(partyDetailFetchIfNeeded(nextProps.partyRegion.selectedPartyID));
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'party', 2)) {
                this.setState({}, () => {
                    var el = ReactDOM.findDOMNode(this.refs.partyDetail)
                    setInputFocus(el, false)
                    focusWasSet()
                })
            }
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);

        switch (action) {
            case 'xxx':
                break
        }
    }

    /**
     * CHANGE VALUE
     * *********************************************
     * Zpracování změny nějaké hodnoty ve formuláři
     * @param event - událost změny
     */ 
    changeValue(event){
        var value = event.target.value;                             // hodnota změna         
        var variable = event.target.name;                           // políčko (název hodnoty) změny
        var p = this.props.partyRegion.selectedPartyData;           // původní osoba
        var party = this.mergePartyChanges(p, variable, value);     // osoba po změne 
        this.setState({party: party});                              // znovuvykresleni formuláře se změnou
    }

    /**
     * UPDATE VALUE
     * *********************************************
     * Uložení změny nekteré hodnoty
     * @param event - událost, která změnu vyvolala
     */ 
    updateValue(event){
        var value = event.target.value;                             // hodnota změna         
        var variable = event.target.name;                           // políčko (název hodnoty) změny
        var party = this.props.partyRegion.selectedPartyData;       // původní osoba
        party = this.mergePartyChanges(party, variable, value);     // osoba po změne 
        if(
            !party.from ||
            party.from.textDate == "" || 
            party.from.textDate == null || 
            party.from.textDate == undefined
        ){  
            party.from = null;                                      // pokud není zadaný textová část data, celý fatum se ruší
        }
        if(
            !party.to || 
            party.to.textDate == "" || 
            party.to.textDate == null || 
            party.to.textDate == undefined
        ){  
            party.to = null;                                        // pokud není zadaný textová část data, celý fatum se ruší
        }
        this.dispatch(updateParty(party));                          // uložení změn a znovu načtení dat osoby   
        this.setState({party:null});
    }

    /**
     * MERGE PARTY CHANGES
     * *********************************************
     * Sloučí změnu v jednom políčku s původním objektem osoby
     * @param party - původní osoba
     * @param variable - název měné hodnoty 
     * @param value - nová hoddnota měnené položky
     */ 
    mergePartyChanges(party, variable, value){
        if((variable=="fromText" || variable=="fromCalendar") && !party.from){  // pokud neni definovane datumove pole a je aktualizováno
            party.from={
                calendarTypeId: this.props.partyRegion.gregorianCalendarId,     // nastaví se mu defaultně gregoriánský kalendář
                textDate: ""                                                    // a prázdný text
            };
        }
        if((variable=="toText" || variable=="toCalendar") && !party.to){        // pokud neni definovane datumove pole a je aktualizováno
            party.to={
                calendarTypeId: this.props.partyRegion.gregorianCalendarId,     // nastaví se mu defaultně gregoriánský kalendář
                textDate: ""                                                    // a prázdný text
            };
        }
        switch(variable){
            case "history" : party.history = value; break;
            case "sourceInformation" : party.sourceInformation = value; break;
            case "fromText" : party.from.textDate = value;  break;
            case "toText" : party.to.textDate = value;  break;
            case "fromCalendar" : party.from.calendarTypeId = value;  break;
            case "toCalendar" : party.to.calendarTypeId = value;  break;
            case "genealogy" : party.genealogy = value; break;
            case "organization" : party.organization = value; break;
            case "foundingNorm" : party.foundingNorm = value; break;
            case "scopeNorm" : party.scopeNorm = value; break;
            case "scope" : party.scope = value; break;
            case "note" : party.record.note = value; break;
            case "characteristics" : party.characteristics = value; break;
        };
        return party;
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení detailu osoby
     */ 
    render() {

        var party = this.props.partyRegion.selectedPartyData;

        if(this.props.partyRegion.isFetchingDetail && party == undefined){
            return <div>{i18n('party.detail.finding')}</div>
        }

        if(party == undefined){
            return <div className="partyDetail">{i18n('party.detail.noSelection')}</div>
        }


        return (
            <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
                <div ref='partyDetail' className={"partyDetail"}>
                    <h1>{party.record.record}</h1>
                    <div className="line">
                    <Input type="textarea" label={i18n('party.detail.characteristics')} name="characteristics" value={party.characteristics != undefined ? party.characteristics : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                    </div>

                    <div className="line typ">
                        <Input type="select" disabled={true} value={party.partyType.partyTypeId} label={i18n('party.detail.type')}>
                            {this.props.refTables.partyTypes.items.map(i=> {return <option key={i.partyTypeId} value={i.partyTypeId}>{i.name}</option>})}
                        </Input>
                    </div>
                    <div className="line datation">
                        <div className="date-group">
                            <div>
                                <label>{i18n('party.nameValidFrom')}</label>
                                <div className="date">
                                    <Input type="select" name="fromCalendar" value={party.from == null || party.from.calendarTypeId == null ? 0 : party.from.calendarTypeId} onChange={this.updateValue} >
                                        {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                    </Input>
                                    <Input type="text"  name="fromText" value={party.from == null || party.from.textDate == null ? '' : party.from.textDate} onChange={this.changeValue} onBlur={this.updateValue} />
                                </div>
                            </div>
                            <div>
                                <label>{i18n('party.nameValidTo')}</label>
                                <div className="date">
                                    <Input type="select" name="toCalendar" value={party.to == null || party.to.calendarTypeId == null ? 0 : party.to.calendarTypeId} onChange={this.updateValue} >
                                        {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                    </Input>
                                    <Input type="text" name="toText" value={party.to == null || party.to.textDate == null ? '' : party.to.textDate} onChange={this.changeValue} onBlur={this.updateValue} />
                                </div>
                            </div>
                        </div>
                    </div>                       

                    <div className="line party-names">
                        <label>{i18n('party.detail.names')}</label>
                        <PartyDetailNames partyRegion={this.props.partyRegion} /> 
                    </div>

                    {party.partyType.partyTypeId == this.state.groupId ? <div className="line party-identifiers">
                        <label>{i18n('party.detail.identifiers')}</label>
                        <PartyDetailIdentifiers partyRegion={this.props.partyRegion} refTables={this.props.refTables}/>
                    </div> :  null}


                    {party.partyType.partyTypeId == this.state.dynastyId ? <div className="line">
                        <Input type="textarea" label={i18n('party.detail.genealogy')} name="genealogy" value={party.genealogy != undefined ? party.genealogy : ''} onChange={this.changeValue} onBlur={this.updateValue}/> </div>:  null}

                    <div className="line"><Input type="textarea" label={i18n('party.detail.note')} name="note" value={party.record.note != undefined ? party.record.note : ''} onChange={this.changeValue} onBlur={this.updateValue}/></div>
                    <div className="line"><Input type="textarea" label={i18n('party.detail.history')} name="history" value={party.history != undefined ? party.history : ''} onChange={this.changeValue} onBlur={this.updateValue}/></div>
                    <div className="line"><Input type="textarea" label={i18n('party.detail.sources')} name="sourceInformation" value={party.sourceInformation == null ? '' : party.sourceInformation} onChange={this.changeValue} onBlur={this.updateValue}/></div>

                    {party.partyType.partyTypeId == this.state.groupId ? <div className="line group-panel">
                        <Input type="text" label={i18n('party.detail.groupOrganization')} name="organization" value={party.organization != undefined ? party.organization : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <Input type="text" label={i18n('party.detail.groupFoundingNorm')} name="foundingNorm" value={party.foundingNorm != undefined ? party.foundingNorm : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <Input type="text" label={i18n('party.detail.groupScopeNorm')} name="scopeNorm" value={party.scopeNorm != undefined ? party.scopeNorm : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <Input type="text" label={i18n('party.detail.groupScope')} name="scope" value={party.scope != undefined ? party.scope : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                    </div> :  ''}

                    <div className="line party-creators">
                        <label>{i18n('party.detail.creators')}</label>
                        <PartyDetailCreators partyRegion={this.props.partyRegion} refTables={this.props.refTables}/> 
                    </div>
                </div>
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {partyRegion, focus} = state
    return {
        partyRegion,
        focus
    }
}

PartyDetail.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(PartyDetail);
