import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {PartyDetailCreators, PartyDetailIdentifiers, PartyDetailNames, AddPartyNameForm, AbstractReactComponent, Search, i18n, FormInput, NoFocusButton, Icon} from 'components/index.jsx';
import {Panel, PanelGroup, FormControl, FormGroup} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {updateParty} from 'actions/party/party.jsx'
import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {Utils} from 'components/index.jsx';
import {objectById} from 'stores/app/utils.jsx';
import {setInputFocus} from 'components/Utils.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

const keyModifier = Utils.getKeyModifier();

const keymap = {
    PartyDetail: {}
};
const shortcutManager = new ShortcutsManager(keymap);

import './PartyDetail.less';

const PARTY_TYPE_IDENT = "ident";
const PARTY_TYPE_CONCLUSION = "conclusion";
const PARTY_TYPE_CORPORATION_CODE = 'GROUP_PARTY';

/**
 * Komponenta detailu osoby
 */
class PartyDetail extends AbstractReactComponent {

    state = {
        activeIndexes: {},
    };

    static PropTypes = {
        focus: React.PropTypes.object.isRequired,
    };

    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        //this.trySetFocus();
        //this.initCalendarTypes(this.props, this.props);
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        //this.trySetFocus(nextProps);
        ///this.initCalendarTypes(this.props, nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {partyDetail: {id}} = props;
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        if (id) {
            this.dispatch(partyDetailFetchIfNeeded(id));
        }
    };

    /**
     * TODO Revert Petr
     */
    /*trySetFocus = (props = this.props) => {
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
    };*/

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
        // Not defined shortcuts
    };

    handleActive = (index) => {
        this.setState({activeIndexes:{...this.state.activeIndexes, [index]: !this.state.activeIndexes[index]}})
    };

    getPartyType = () => {
        return objectById(this.props.partyTypes.items, this.props.partyDetail.data.partyType.partyTypeId, 'partyTypeId');
    };

    handleNameAdd = () => {
        const partyType = this.getPartyType();
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.new') , <AddPartyNameForm partyType={partyType} />));
    };

    render() {
        const {userDetail, partyDetail, refTables: {calendarTypes}} = this.props;
        //const {fromCalendar, toCalendar} = this.state;
        const party = partyDetail.data;

        if (!party) {

            if (partyDetail.isFetching) {
                return <div>{i18n('party.detail.finding')}</div>
            }

            return <div className="unselected-msg">
                <div className="title">{i18n('party.noSelection.title')}</div>
                <div className="msg-text">{i18n('party.noSelection.message')}</div>
            </div>
        }

        let canEdit = userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: party.record.scopeId});

        const parts = [
            {
                type:PARTY_TYPE_IDENT
            },{
                type:"GENERAL",
                name:"Stručná charakteristika"
            },{
                type:"GENERAL",
                name:"Životopisné údaje"
            },{
                type:"GENERAL",
                name:"Vztahy"
            },{
                type:"GENERAL",
                name:"Poznámka"
            },{
                type:PARTY_TYPE_CONCLUSION,
                name:"Zdroje informací, autoři"
            }
        ];

        const partyType = this.getPartyType();

        return <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
            <div ref='partyDetail' className="party-detail">
                <div className="party-header">
                    <div>
                        <h3>{party.name}</h3>
                        {party.record.external_id && party.record.externalSource && <span className="description">{party.partyType.description + ':' + party.partyId}</span>}
                        {party.record.external_id && !party.record.externalSource && <span className="description">{'UNKNOWN:' + party.record.external_id}</span>}
                        {!party.record.external_id && <span className="description">{party.partyType.description + ':' + party.partyId}</span>}
                    </div>
                    <div>
                        {party.partyType.description}
                    </div>
                </div>
                <div className="party-body">
                    {parts.map((i, index) => {
                        if (i.type == PARTY_TYPE_IDENT) {
                            return <div>
                                <PanelGroup activeKey={this.state.activeIndexes[PARTY_TYPE_IDENT + '_FORM_NAMES'] ? PARTY_TYPE_IDENT + '_FORM_NAMES' : PARTY_TYPE_IDENT + '_FORM_NAMES'} onSelect={this.handleActive} accordion>
                                    <Panel header={<div>Formy jména<NoFocusButton className="pull-right hover-button"><Icon glyph="fa-thumb-tack" /></NoFocusButton></div>} eventKey={PARTY_TYPE_IDENT + '_FORM_NAMES'}>
                                        <div>
                                            <label>Formy jména</label>
                                            <NoFocusButton bsStyle="default" onClick={this.handleNameAdd}><Icon glyph="fa-plus" /></NoFocusButton>
                                        </div>
                                        {party.partyNames.map((name, index) => {
                                            const res = [];
                                            const hlp = (a,b) => {
                                                if (a) {
                                                    b.push(a);
                                                }
                                            };

                                            console.log("val", name);
                                            hlp(name.degreeBefore, res);
                                            hlp(name.mainPart, res);
                                            hlp(name.otherPart, res);
                                            let roman = null, geoAddon = null, addon = null;
                                            name.partyNameComplements.forEach((e) => {
                                                const type = objectById(partyType.complementTypes, e.complementTypeId, 'complementTypeId');
                                                if (type) {
                                                    if (type.code == "2") {
                                                        addon = e.complement;
                                                    } else if (type.code == "3") {
                                                        roman = e.complement;
                                                    } else if (type.code == "4") {
                                                        geoAddon = e.complement;
                                                    }
                                                }
                                            });
                                            hlp(roman, res);
                                            hlp(geoAddon, res);
                                            hlp(addon, res);
                                            let str = res.join(' ')
                                            if (name.degreeAfter != null && name.degreeAfter.length > 0) {
                                                str += ', ' + name.degreeAfter;
                                            }
                                            return <div>
                                                <FormControl.Static>{str}</FormControl.Static>
                                                <NoFocusButton><Icon glyph="fa-pencil" /></NoFocusButton>
                                                <NoFocusButton><Icon glyph="fa-times" /></NoFocusButton>
                                                {name.prefferedName ? "(Preferovná forma jména)" : <NoFocusButton><Icon glyph="fa-check" /></NoFocusButton>}
                                            </div>
                                        })}
                                    </Panel>
                                </PanelGroup>
                                {party.partyType.code == PARTY_TYPE_CORPORATION_CODE && <PanelGroup activeKey={this.state.activeIndexes[PARTY_TYPE_IDENT + '_CORP_IDENT'] ? PARTY_TYPE_IDENT + '_CORP_IDENT' : false} onSelect={this.handleActive} accordion>
                                    <Panel header={<div>Identifikátory korporace<NoFocusButton className="pull-right hover-button"><Icon glyph="fa-thumb-tack" /></NoFocusButton></div>} eventKey={PARTY_TYPE_IDENT + '_CORP_IDENT'}>
                                        Body
                                    </Panel>
                                </PanelGroup>}
                            </div>;
                        } else if (i.type == PARTY_TYPE_CONCLUSION) {
                            return <div>
                                <PanelGroup activeKey={this.state.activeIndexes[PARTY_TYPE_CONCLUSION + '_SOURCES'] ? PARTY_TYPE_CONCLUSION + '_SOURCES' : false} onSelect={this.handleActive} accordion>
                                    <Panel header={<div>Zdroje informací<NoFocusButton className="pull-right hover-button"><Icon glyph="fa-thumb-tack" /></NoFocusButton></div>} eventKey={PARTY_TYPE_CONCLUSION + '_SOURCES'}>{/* TBD */}</Panel>
                                </PanelGroup>
                                <PanelGroup activeKey={this.state.activeIndexes[PARTY_TYPE_CONCLUSION + '_CREATORS'] ? PARTY_TYPE_CONCLUSION + '_CREATORS' : false} onSelect={this.handleActive} accordion>
                                    <Panel header={<div>Autoři<NoFocusButton className="pull-right hover-button"><Icon glyph="fa-thumb-tack" /></NoFocusButton></div>} eventKey={PARTY_TYPE_CONCLUSION + '_CREATORS'}>{/* TBD */}</Panel>
                                </PanelGroup>
                            </div>;
                        }
                        return <PanelGroup activeKey={this.state.activeIndexes[index] ? index : false} onSelect={this.handleActive} accordion>
                            <Panel header={<div>{i.name}<NoFocusButton className="pull-right hover-button"><Icon glyph="fa-thumb-tack" /></NoFocusButton></div>} eventKey={index}>{/* TBD */}</Panel>
                        </PanelGroup>
                    })}
                </div>
            </div>
        </Shortcuts>;
    }
}

function mapStateToProps(state) {
    const {app: {partyDetail}, focus, userDetail, refTables: {partyTypes}} = state;
    return {
        partyDetail,
        focus,
        userDetail,
        partyTypes
    }
}

export default connect(mapStateToProps)(PartyDetail);
