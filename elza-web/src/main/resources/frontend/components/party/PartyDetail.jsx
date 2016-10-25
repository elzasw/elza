/**
 * Komponenta detailu osoby
 */
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
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

const keyModifier = Utils.getKeyModifier();

const keymap = {
    PartyDetail: {
        xxx: keyModifier + 'e',
    },
};
const shortcutManager = new ShortcutsManager(keymap);

import './PartyFormStyles.less';

/**
 * PARTY DETAIL
 * *********************************************
 * Detail osoby
 */
class PartyDetail extends AbstractReactComponent {

    static PropTypes = {
        focus: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
    };

    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    state = {
        dynastyId: 2,
        groupId: 3,
    };

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        this.trySetFocus();
        this.initCalendarTypes(this.props, this.props);
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        this.initCalendarTypes(this.props, nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {partyDetail: {id}} = props;
        if (id) {
            this.dispatch(partyDetailFetchIfNeeded(id));
        }
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, 'party', 2)) {
                this.setState({}, () => {
                    var el = ReactDOM.findDOMNode(this.refs.partyDetail)
                    setInputFocus(el, false)
                    focusWasSet()
                })
            }
        }
    };

    /**
     * Inicializace typů kalendáře (z důvodu možnosti výběru typu kalendáře, když není zadaná hodnota datumu. Nedochází k ukládání osoby)
     */
    initCalendarTypes = (props, nextProps)  => {
        const party = nextProps.partyDetail.data;
        let {fromCalendar, toCalendar} = this.state;

        var partyChanged = props.partyDetail.id != nextProps.partyDetail.id;
        if (partyChanged) {
            fromCalendar = null;
            toCalendar = null;
        }


        if (party) {
            if (party.from != undefined && party.from.calendarTypeId != undefined) {
                fromCalendar = party.from.calendarTypeId;
            }
            fromCalendar = fromCalendar == undefined ? "A" : fromCalendar;

            if (party.to != undefined && party.to.calendarTypeId != undefined) {
                toCalendar = party.to.calendarTypeId;
            }
            toCalendar = toCalendar == undefined ? "A" : toCalendar;
        }

        this.setState({toCalendar, fromCalendar});
    };

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts = (action)  => {
        console.log("#handleShortcuts", '[' + action + ']', this);

        switch (action) {
            case 'xxx':
                break
        }
    };

    /**
     * CHANGE VALUE
     * *********************************************
     * Zpracování změny nějaké hodnoty ve formuláři
     * @param event - událost změny
     */ 
    changeValue = (needUpdate, event) => {
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
    };

    /**
     * UPDATE VALUE
     * *********************************************
     * Uložení změny nekteré hodnoty
     * @param event - událost, která změnu vyvolala
     */ 
    updateValue = (event) => {
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
    };

    /**
     * MERGE PARTY CHANGES
     * *********************************************
     * Sloučí změnu v jednom políčku s původním objektem osoby
     * @param party - původní osoba
     * @param variable - název měné hodnoty 
     * @param value - nová hoddnota měnené položky
     */ 
    mergePartyChanges = (party, variable, value) => {
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
        }
        return party;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení detailu osoby
     */ 
    render() {
        const {userDetail, partyDetail, refTables: {partyTypes, calendarTypes}} = this.props;
        const {fromCalendar, toCalendar} = this.state;
        const party = partyDetail.data;

        if (partyDetail.isFetching && !party) {
            return <div>{i18n('party.detail.finding')}</div>
        }

        if (!party) {
            return <div className="unselected-msg">
                <div className="title">{i18n('party.noSelection.title')}</div>
                <div className="msg-text">{i18n('party.noSelection.message')}</div>
            </div>
        }

        let canEdit = userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: party.record.scopeId});

        return (
            <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
                <div ref='partyDetail' className="partyDetail">
                    <h1>{party.record.record}</h1>
                    <div className="line">
                        <FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.characteristics')} name="characteristics" value={party.characteristics ? party.characteristics : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                    </div>
                    <div className="line typ">
                        <FormInput componentClass="select" disabled={true} value={party.partyType.partyTypeId} label={i18n('party.detail.type')}>
                            {partyTypes.items.map(i=> {return <option key={i.partyTypeId} value={i.partyTypeId}>{i.name}</option>})}
                        </FormInput>
                    </div>
                    <div className="line datation">
                        <div className="date-group">
                            <div>
                                <label>{i18n('party.nameValidFrom')}</label>
                                <div className="date">
                                    <FormInput disabled={!canEdit} componentClass="select" name="fromCalendar" value={fromCalendar} onChange={this.changeValue.bind(this, party.from && party.from.textDate)} onBlur={this.updateValue}>
                                        {calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                    </FormInput>
                                    <FormInput disabled={!canEdit} type="text"  name="fromText" value={party.from == null || party.from.textDate == null ? '' : party.from.textDate} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue} />
                                </div>
                            </div>
                            <div>
                                <label>{i18n('party.nameValidTo')}</label>
                                <div className="date">
                                    <FormInput disabled={!canEdit} componentClass="select" name="toCalendar" value={toCalendar} onChange={this.changeValue.bind(this, party.to && party.to.textDate)} onBlur={this.updateValue} >
                                        {calendarTypes.items.map(i=> {return <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>})}
                                    </FormInput>
                                    <FormInput disabled={!canEdit} type="text" name="toText" value={party.to == null || party.to.textDate == null ? '' : party.to.textDate} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue} />
                                </div>
                            </div>
                        </div>
                    </div>                       

                    <div className="line party-names">
                        <label>{i18n('party.detail.names')}</label>
                        {false && <PartyDetailNames canEdit={canEdit} party={partyDetail.data}  />}
                    </div>

                    {false && party.partyType.partyTypeId == this.state.groupId ? <div className="line party-identifiers">
                        <label>{i18n('party.detail.identifiers')}</label>
                        <PartyDetailIdentifiers canEdit={canEdit} party={partyDetail.data} refTables={refTables}/>
                    </div> :  null}


                    {false && party.partyType.partyTypeId == this.state.dynastyId ? <div className="line">
                        <FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.genealogy')} name="genealogy" value={party.genealogy ? party.genealogy : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/> </div>:  null}

                    <div className="line"><FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.note')} name="note" value={party.record.note ? party.record.note : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/></div>
                    <div className="line"><FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.history')} name="history" value={party.history ? party.history : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/></div>
                    <div className="line"><FormInput disabled={!canEdit} componentClass="textarea" label={i18n('party.detail.sources')} name="sourceInformation" value={party.sourceInformation == null ? '' : party.sourceInformation} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/></div>

                    {false && party.partyType.partyTypeId == this.state.groupId ? <div className="line group-panel">
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupOrganization')} name="organization" value={party.organization ? party.organization : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupFoundingNorm')} name="foundingNorm" value={party.foundingNorm ? party.foundingNorm : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupScopeNorm')} name="scopeNorm" value={party.scopeNorm ? party.scopeNorm : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                        <FormInput disabled={!canEdit} type="text" label={i18n('party.detail.groupScope')} name="scope" value={party.scope ? party.scope : ''} onChange={this.changeValue.bind(this,true)} onBlur={this.updateValue}/>
                    </div> :  ''}

                    <div className="line party-creators">
                        <label>{i18n('party.detail.creators')}</label>
                        {false && <PartyDetailCreators canEdit={canEdit} partyRegion={this.props.partyRegion} refTables={this.props.refTables}/>}
                    </div>
                </div>
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {app: {partyDetail}, focus, userDetail} = state;
    return {
        partyDetail,
        focus,
        userDetail
    }
}

export default connect(mapStateToProps)(PartyDetail);
