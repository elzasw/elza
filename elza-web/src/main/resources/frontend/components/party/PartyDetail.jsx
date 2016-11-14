import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form'
import {
    PartyDetailCreators,
    PartyIdentifierForm,
    PartyDetailIdentifiers,
    PartyDetailNames,
    PartyDetailRelations,
    PartyNameForm,
    PartyField,
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    NoFocusButton,
    Icon,
    CollapsablePanel
} from 'components/index.jsx';
import {Form} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyUpdate} from 'actions/party/party.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded, PARTY_TYPE_CODES} from 'actions/party/party.jsx'
import {Utils} from 'components/index.jsx';
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {initForm} from "actions/form/inlineForm.jsx"

const keyModifier = Utils.getKeyModifier();

const keymap = {
    PartyDetail: {}
};
const shortcutManager = new ShortcutsManager(keymap);

import './PartyDetail.less';


const SETTINGS_PARTY_PIN = "PARTY_PIN";

const UI_PARTY_GROUP_TYPE = {
    GENERAL: 'GENERAL',
    CONCLUSION: 'CONCLUSION',
    IDENT: 'IDENT'
}

/**
 * Komponenta detailu osoby
 */
class PartyDetail extends AbstractReactComponent {

    state = {
        activeIndexes: {},
        visibilitySettings: {},
        visibilitySettingsValue: {}
    };

    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    static fields = [
        'sourceInformation',
        'history',
        'source_information',
        'characteristics',
        'genealogy',
        'scope',
        'foundingNorm',
        'scopeNorm',
        'organization',
        'creators[]'
    ];

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required')
            }
            return errors
        }, {});

    static validate = (values) => {
        const errors = {};

        // TODO @compel validation

        return errors;
    };

    componentDidMount() {
        this.trySetFocus(); // TODO @compel focus
        this.fetchIfNeeded();
        this.updateStateFromProps();
        this.props.initForm(this.handlePartyUpdate);
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps); // TODO @compel focus
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

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus() && focus) {
            if (isFocusFor(focus, 'party', 2)) {
                this.setState({}, () => {
                    const el = ReactDOM.findDOMNode(this.refs.partyDetail);
                    setInputFocus(el, false);
                    focusWasSet()
                })
            }
        }
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
        return objectById(this.props.partyTypes.items, this.props.partyDetail.data.partyType.id, 'id');
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
        this.dispatch(partyUpdate({
            ...this.props.partyDetail.data,
            ...party
        }));
    };

    render() {
        const {userDetail, partyDetail, fields: {sourceInformation, creators}} = this.props;
        const fields = this.props.fields;
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

        const partyType = this.getPartyType();

        let parts = partyType && partyType.partyGroups ? partyType.partyGroups : [];

        const events = {onPin:this.handlePinToggle, onSelect: this.handleToggleActive};

        return <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
            <div ref='partyDetail' className="party-detail">
                <div className="party-header">
                    <div>
                        <h3>{party.name}</h3>
                        {party.record.external_id && party.record.externalSource && <span className="description">{party.partyType.description + ':' + party.id}</span>}
                        {party.record.external_id && !party.record.externalSource && <span className="description">{'UNKNOWN:' + party.record.external_id}</span>}
                        {!party.record.external_id && <span className="description">{party.partyType.description + ':' + party.id}</span>}
                    </div>
                    <div>
                        <h3>{party.partyType.description}</h3>
                        <div>{dateTimeToString(new Date(party.record.lastUpdate))}</div>
                    </div>
                </div>
                <Form className="party-body">
                    {parts.map((i, index) => {
                        const TYPE = i.type.toUpperCase();

                        if (TYPE == UI_PARTY_GROUP_TYPE.IDENT) {
                            const key = UI_PARTY_GROUP_TYPE.IDENT + '_FORM_NAMES';
                            const corpKey = UI_PARTY_GROUP_TYPE.IDENT + '_CORP_IDENT';
                            return <div key={index}>
                                <CollapsablePanel isOpen={activeIndexes[key]} pinned={visibilitySettingsValue[key]} header={i18n("party.detail.formNames")} eventKey={key} {...events}>
                                    <PartyDetailNames party={party} partyType={partyType} onPartyUpdate={this.handlePartyUpdate} />
                                </CollapsablePanel>
                                {party.partyType.code == PARTY_TYPE_CODES.GROUP_PARTY && <CollapsablePanel isOpen={activeIndexes[corpKey]} pinned={visibilitySettingsValue[corpKey]} header={i18n("party.detail.partyGroupIdentifiers")} eventKey={corpKey} {...events}>
                                    <PartyDetailIdentifiers party={party} onPartyUpdate={this.handlePartyUpdate} />
                                </CollapsablePanel>}
                            </div>;
                        } else if (TYPE == UI_PARTY_GROUP_TYPE.CONCLUSION) {
                            const sourcesKey = UI_PARTY_GROUP_TYPE.CONCLUSION + '_SOURCES';
                            const creatorsKey = UI_PARTY_GROUP_TYPE.CONCLUSION + '_CREATORS';
                            return <div key={index}>
                                <CollapsablePanel isOpen={activeIndexes[sourcesKey]} pinned={visibilitySettingsValue[sourcesKey]} header={i18n("party.detail.sources")} eventKey={sourcesKey} {...events}>
                                    <FormInput componentClass="textarea" {...sourceInformation} />
                                </CollapsablePanel>
                                <CollapsablePanel isOpen={activeIndexes[creatorsKey]} pinned={visibilitySettingsValue[creatorsKey]} header={i18n("party.detail.creators")} eventKey={creatorsKey} {...events}>
                                    <div>{creators.map((creator, index) => <div key={index +"-"+creator.id} className="value-group">
                                        <PartyField {...creator} />
                                        <NoFocusButton bsStyle="default" onClick={() => {
                                            if (confirm(i18n('party.detail.creator.delete'))) {
                                                creators.removeField(index)
                                            }
                                        }}><Icon glyph="fa-trash" /></NoFocusButton>
                                    </div>)}</div>
                                    <NoFocusButton bsStyle="default" onClick={() => creators.addField({})}><Icon glyph="fa-plus" /></NoFocusButton>
                                </CollapsablePanel>
                            </div>;
                        } else if (TYPE === UI_PARTY_GROUP_TYPE.GENERAL) {
                            const items = [];

                            if (i.contentDefinition) {
                                let contentDefinition;
                                try {
                                    contentDefinition = JSON.parse(i.contentDefinition);
                                } catch (e) {
                                    console.error(e);
                                    contentDefinition = null
                                }
                                if (contentDefinition) {
                                    for (let key in contentDefinition) {
                                        if (contentDefinition.hasOwnProperty(key)) {
                                            const item = contentDefinition[key];
                                            const inputProps = {
                                                ...fields[item.definition],
                                                label: item.name,
                                                title: item.desc
                                            };

                                            let element = null;

                                            if (item.type === "text") {
                                                element = <FormInput {...inputProps} type={item.type}/>
                                            } else if (item.type === "textarea") {
                                                element = <FormInput {...inputProps} componentClass={item.type}/>
                                            } else if (item.type === "relation") {
                                                const type = objectById(partyType.relationTypes, item.definition, 'code');
                                                if (type) {
                                                    element = <PartyDetailRelations party={party} relationType={type}
                                                                                    label={item.name}/>
                                                } else {
                                                    element = "Neznámý typ relace"
                                                }
                                            }

                                            items.push(<div key={key} className={"el-" + (item.width ? item.width : 0)}>
                                                {element}
                                            </div>);
                                        }
                                    }
                                } else {
                                    items.push(<span>{i18n('party.detail.uiDefinitionError')}</span>)
                                }
                            }

                            return <CollapsablePanel key={index} isOpen={activeIndexes[index]}
                                                     pinned={visibilitySettingsValue[index]} header={i.name}
                                                     eventKey={index} {...events}>
                                <div className="elements-container">
                                    {items}
                                </div>
                            </CollapsablePanel>
                        } else {
                            return <span>{i18n('party.detail.ui.unknownType', i.type)}</span>
                        }
                    })}
                </Form>
            </div>
        </Shortcuts>;
    }
}

export default reduxForm({
        form: 'partyDetail',
        fields: PartyDetail.fields,
        validate: PartyDetail.validate
    },(state) => {
        const {app: {partyDetail}, focus, userDetail, refTables: {partyTypes}} = state;
        return {
            partyDetail,
            focus,
            userDetail,
            partyTypes,
            initialValues: partyDetail.fetched ? partyDetail.data : {}
        }
    },
    {initForm: (onSave) => (initForm('partyDetail', PartyDetail.validate, onSave))}
)(PartyDetail);
