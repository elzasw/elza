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
    PartyDetail: {}
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

    /**
     * RENDER
     * *********************************************
     * Vykreslení detailu osoby
     */ 
    render() {
        const {userDetail, partyDetail, refTables: {partyTypes, calendarTypes}} = this.props;
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

        return <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
            <div ref='partyDetail' className="party-detail">
                <div className="party-header">
                    {/* TODO @compel */}
                </div>
            </div>
        </Shortcuts>;
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
