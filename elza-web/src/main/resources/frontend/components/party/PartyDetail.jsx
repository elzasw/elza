import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    PartyDetailCreators,
    PartyIdentifierForm,
    PartyDetailIdentifiers,
    PartyDetailNames,
    PartyNameForm,
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    NoFocusButton,
    Icon,
    CollapsablePanel
} from 'components/index.jsx';
import {FormControl} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyUpdate} from 'actions/party/party.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {Utils} from 'components/index.jsx';
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
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

const SETTINGS_PARTY_PIN = "PARTY_PIN";

/**
 * Komponenta detailu osoby
 */
class PartyDetail extends AbstractReactComponent {

    state = {
        activeIndexes: {[PARTY_TYPE_IDENT + '_FORM_NAMES']: true},
        visibilitySettings: {},
        visibilitySettingsValue: {}
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
        this.updateStateFromProps();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        //this.trySetFocus(nextProps);
        ///this.initCalendarTypes(this.props, nextProps);
        this.updateStateFromProps(nextProps);
    }

    updateStateFromProps(props = this.props) {
        if (props.userDetail && props.userDetail.settings) {
            const {settings} = props.userDetail;
            const visibilitySettings = getOneSettings(settings, SETTINGS_PARTY_PIN);

            let activeIndexes, visibilitySettingsValue = {};
            if (visibilitySettings.value) {
                visibilitySettingsValue = JSON.parse(visibilitySettings.value);
                activeIndexes = {
                    ...this.state.activeIndexes,
                    ...visibilitySettingsValue
                };
            } else {
                console.warn("No settings for visibility - fallback to default - closed");
                activeIndexes = {};
            }
            this.setState({visibilitySettings, activeIndexes, visibilitySettingsValue})
        }
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

    handleToggleActive = (index) => {
        if (!this.state.visibilitySettingsValue[index]) {
            this.setState({activeIndexes:{...this.state.activeIndexes, [index]: !this.state.activeIndexes[index]}})
        }
    };

    getPartyType = () => {
        return objectById(this.props.partyTypes.items, this.props.partyDetail.data.partyType.partyTypeId, 'partyTypeId');
    };


    handlePinToggle = (identificator) => {
        const oldSettings = getOneSettings(this.props.userDetail.settings, SETTINGS_PARTY_PIN);
        const value = oldSettings.value ? JSON.parse(oldSettings.value) : {};
        const newVisibilitySettings = {
            id: oldSettings.id ? oldSettings.id : null,
            ...oldSettings,
            value: JSON.stringify({
                ...value,
                [identificator]: !value[identificator]
            })
        };
        let newSettings = this.props.userDetail.settings ? [...this.props.userDetail.settings] : [];
        newSettings = setSettings(newSettings, newVisibilitySettings.id, newVisibilitySettings);
        this.dispatch(userDetailsSaveSettings(newSettings))
    };


    handlePartyUpdate = (party) => {
        this.dispatch(partyUpdate(party));
    };

    render() {
        const {userDetail, partyDetail, refTables: {calendarTypes}} = this.props;
        //const {fromCalendar, toCalendar} = this.state;
        const party = partyDetail.data;
        const {activeIndexes, visibilitySettingsValue} = this.state;

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

        const events = {onPin:this.handlePinToggle, onSelect: this.handleToggleActive};

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
                        <h3>{party.partyType.description}</h3>
                        <div>{dateTimeToString(new Date(party.record.lastUpdate))}</div>
                    </div>
                </div>
                <div className="party-body">
                    {parts.map((i, index) => {
                        if (i.type == PARTY_TYPE_IDENT) {
                            const key = PARTY_TYPE_IDENT + '_FORM_NAMES';
                            const corpKey = PARTY_TYPE_IDENT + '_CORP_IDENT';
                            return <div key={index}>
                                <CollapsablePanel isOpen={activeIndexes[key]} pinned={visibilitySettingsValue[key]} header={i18n("party.detail.formNames")} eventKey={key} {...events}>
                                    <PartyDetailNames party={party} partyType={partyType} onPartyUpdate={this.handlePartyUpdate} />
                                </CollapsablePanel>
                                {party.partyType.code == PARTY_TYPE_CORPORATION_CODE && <CollapsablePanel isOpen={activeIndexes[corpKey]} pinned={visibilitySettingsValue[corpKey]} header={i18n("party.detail.partyGroupIdentifiers")} eventKey={corpKey} {...events}>
                                    <PartyDetailIdentifiers party={party} onPartyUpdate={this.handlePartyUpdate} />
                                </CollapsablePanel>}
                            </div>;
                        } else if (i.type == PARTY_TYPE_CONCLUSION) {
                            const sourcesKey = PARTY_TYPE_CONCLUSION + '_SOURCES';
                            const creatorsKey = PARTY_TYPE_CONCLUSION + '_CREATORS';
                            return <div key={index}>
                                <CollapsablePanel isOpen={activeIndexes[sourcesKey]} pinned={visibilitySettingsValue[sourcesKey]} header={i18n("party.detail.sources")} eventKey={sourcesKey} {...events}>
                                    {/* TBD */}
                                </CollapsablePanel>
                                <CollapsablePanel isOpen={activeIndexes[creatorsKey]} pinned={visibilitySettingsValue[creatorsKey]} header={i18n("party.detail.creators")} eventKey={creatorsKey} {...events}>
                                    {/* TBD */}
                                </CollapsablePanel>
                            </div>;
                        }
                        return <CollapsablePanel key={index} isOpen={activeIndexes[index]} pinned={visibilitySettingsValue[index]} header={i.name} eventKey={index} {...events}>
                            {/* TBD */}
                        </CollapsablePanel>
                    })}
                </div>
            </div>
        </Shortcuts>;
    }
}

export default connect((state) => {
    const {app: {partyDetail}, focus, userDetail, refTables: {partyTypes}} = state;
    return {
        partyDetail,
        focus,
        userDetail,
        partyTypes
    }
})(PartyDetail);
