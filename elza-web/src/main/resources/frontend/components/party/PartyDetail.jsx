/**
 * Komponenta detailu osoby
 */

require('./PartyFormStyles.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {PartyDetailCreators, PartyDetailIdentifiers, PartyDetailNames, AbstractReactComponent, Search, i18n, FormInput} from 'components/index.jsx';
import {AppActions} from 'stores/index.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {updateParty} from 'actions/party/party.jsx'
import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {Utils} from 'components/index.jsx';
import {setInputFocus} from 'components/Utils.jsx'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

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
            'trySetFocus',
            'initCalendarTypes'
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
        this.initCalendarTypes(this.props, this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(partyDetailFetchIfNeeded(nextProps.partyRegion.selectedPartyID));
        this.trySetFocus(nextProps)
        this.initCalendarTypes(this.props, nextProps);
    }

    trySetFocus(props) {
        var {focus} = props

        var callback = null;
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

    /**
     * Inicializace typů kalendáře (z důvodu možnosti výběru typu kalendáře, když není zadaná hodnota datumu. Nedochází k ukládání osoby)
     */
    initCalendarTypes(props, nextProps) {
        var party = nextProps.partyRegion.selectedPartyData;
        var fromCalendar = this.state.fromCalendar;
        var toCalendar = this.state.toCalendar;

        var partyChanged = props.partyRegion.selectedPartyID != nextProps.partyRegion.selectedPartyID;
        if(partyChanged){
            fromCalendar = null;
            toCalendar = null;
        }


        if (party != undefined) {
            if (party.from != undefined && party.from.calendarTypeId != undefined) {
                fromCalendar = party.from.calendarTypeId;
            }
            fromCalendar = fromCalendar == undefined ? nextProps.partyRegion.gregorianCalendarId : fromCalendar;

            if (party.to != undefined && party.to.calendarTypeId != undefined) {
                toCalendar = party.to.calendarTypeId;
            }
            toCalendar = toCalendar == undefined ? nextProps.partyRegion.gregorianCalendarId : toCalendar;
        }

        this.setState({
            toCalendar: toCalendar,
            fromCalendar: fromCalendar});
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
    changeValue(needUpdate, event){
        var value = event.target.value;                             // hodnota změna         
        var variable = event.target.name;                           // políčko (název hodnoty) změny
        var p = this.props.partyRegion.selectedPartyData;           // původní osoba
        var party = this.mergePartyChanges(p, variable, value);     // osoba po změne 
        this.setState({
            toCalendar: p.to ? p.to.calendarTypeId : this.state.toCalendar,
            fromCalendar: p.from ? p.from.calendarTypeId : this.state.fromCalendar,
            needUpdate: needUpdate,
            party: party
        });                              // znovuvykresleni formuláře se změnou
    }

    /**
     * UPDATE VALUE
     * *********************************************
     * Uložení změny nekteré hodnoty
     * @param event - událost, která změnu vyvolala
     */ 
    updateValue(event){
        if(this.state.needUpdate){
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
                party.from = null;                                      // pokud není zadaný textová část data, celý datum se ruší
            }
            if(
                !party.to ||
                party.to.textDate == "" ||
                party.to.textDate == null ||
                party.to.textDate == undefined
            ){
                party.to = null;                                        // pokud není zadaný textová část data, celý datum se ruší
            }
            this.dispatch(updateParty(party));                          // uložení změn a znovu načtení dat osoby
            this.setState({
                needUpdate: false,
                party:null});
        }
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
                calendarTypeId: this.state.fromCalendar ? this.state.fromCalendar : this.props.partyRegion.gregorianCalendarId,     // nastaví se mu defaultně gregoriánský kalendář
                textDate: ""                                                    // a prázdný text
            };
        }
        if((variable=="toText" || variable=="toCalendar") && !party.to){        // pokud neni definovane datumove pole a je aktualizováno
            party.to={
                calendarTypeId: this.state.toCalendar ? this.state.toCalendar : this.props.partyRegion.gregorianCalendarId,     // nastaví se mu defaultně gregoriánský kalendář
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
        const {userDetail} = this.props

        var party = this.props.partyRegion.selectedPartyData;

        if(this.props.partyRegion.isFetchingDetail && party == undefined){
            return <div>{i18n('party.detail.finding')}</div>
        }

        if(party == undefined){
            return(
                    <div className="unselected-msg">
                        <div className="title">{i18n('party.noSelection.title')}</div>
                        <div className="msg-text">{i18n('party.noSelection.message')}</div>                                                
                    </div>
                    );
        }

        var canEdit = false
        if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: party.record.scopeId})) {
            canEdit = true
        }

        return (
            <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
                <div ref='partyDetail' className={"partyDetail"}>
                    <h1>{party.record.record}</h1>
                    <div className="line">
                    <FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.characteristics')} name="characteristics" value={party.characteristics != undefined ? party.characteristics : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                    </div>

                    <div className="line typ">
                        <FormInput componentClass="select" disabled={true} value={party.partyType.partyTypeId} label={i18n('party.detail.type')}>
                            {this.props.refTables.partyTypes.items.map(i=> {return <option key={i.partyTypeId} value={i.partyTypeId}>{i.name}</option>})}
                        </FormInput>
                    </div>
                    <div className="line datation">
                        <div className="date-group">
                            <div>
                                <label>{i18n('party.nameValidFrom')}</label>
                                <div className="date">
                                    <FormInput disabled={!canEdit} componentClass="select" name="fromCalendar" value={this.state.fromCalendar} onChange={this.changeValue.bind(this,party.from != undefined && party.from.textDate != undefined)} onBlur={this.updateValue}>
                                        {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                    </FormInput>
                                    <FormInput disabled={!canEdit} type="text"  name="fromText" value={party.from == null || party.from.textDate == null ? '' : party.from.textDate} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue} />
                                </div>
                            </div>
                            <div>
                                <label>{i18n('party.nameValidTo')}</label>
                                <div className="date">
                                    <FormInput disabled={!canEdit} componentClass="select" name="toCalendar" value={this.state.toCalendar} onChange={this.changeValue.bind(this, party.to != undefined && party.to.textDate != undefined)} onBlur={this.updateValue} >
                                        {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                    </FormInput>
                                    <FormInput disabled={!canEdit} type="text" name="toText" value={party.to == null || party.to.textDate == null ? '' : party.to.textDate} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue} />
                                </div>
                            </div>
                        </div>
                    </div>                       

                    <div className="line party-names">
                        <label>{i18n('party.detail.names')}</label>
                        <PartyDetailNames canEdit={canEdit} partyRegion={this.props.partyRegion} />
                    </div>

                    {party.partyType.partyTypeId == this.state.groupId ? <div className="line party-identifiers">
                        <label>{i18n('party.detail.identifiers')}</label>
                        <PartyDetailIdentifiers canEdit={canEdit} partyRegion={this.props.partyRegion} refTables={this.props.refTables}/>
                    </div> :  null}


                    {party.partyType.partyTypeId == this.state.dynastyId ? <div className="line">
                        <FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.genealogy')} name="genealogy" value={party.genealogy != undefined ? party.genealogy : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/> </div>:  null}

                    <div className="line"><FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.note')} name="note" value={party.record.note != undefined ? party.record.note : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/></div>
                    <div className="line"><FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.history')} name="history" value={party.history != undefined ? party.history : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/></div>
                    <div className="line"><FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.sources')} name="sourceInformation" value={party.sourceInformation == null ? '' : party.sourceInformation} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/></div>

                    {party.partyType.partyTypeId == this.state.groupId ? <div className="line group-panel">
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupOrganization')} name="organization" value={party.organization != undefined ? party.organization : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupFoundingNorm')} name="foundingNorm" value={party.foundingNorm != undefined ? party.foundingNorm : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupScopeNorm')} name="scopeNorm" value={party.scopeNorm != undefined ? party.scopeNorm : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupScope')} name="scope" value={party.scope != undefined ? party.scope : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                    </div> :  ''}

                    <div className="line party-creators">
                        <label>{i18n('party.detail.creators')}</label>
                        <PartyDetailCreators canEdit={canEdit} partyRegion={this.props.partyRegion} refTables={this.props.refTables}/>
                    </div>
                </div>
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {partyRegion, focus, userDetail} = state;
    return {
        partyRegion,
        focus,
        userDetail
    }
}

PartyDetail.propTypes = {
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    partyRegion: React.PropTypes.object.isRequired
};

PartyDetail.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(PartyDetail);
