import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form'
import {
    PartyDetailCreators,
    PartyIdentifierForm,
    PartyDetailIdentifiers,
    PartyDetailNames,
    PartyDetailRelations,
    PartyDetailRelationClass,
    PartyNameForm,
    PartyField,
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    Icon,
    CollapsablePanel
} from 'components/index.jsx';
import {Form, Button} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyUpdate} from 'actions/party/party.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {partyAdd, findPartyFetchIfNeeded, partyDetailFetchIfNeeded, PARTY_TYPE_CODES} from 'actions/party/party.jsx'
import {Utils} from 'components/index.jsx';
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {initForm} from "actions/form/inlineForm.jsx"
import {getMapFromList} from 'stores/app/utils.jsx'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
import {PartyListItem} from 'components/index.jsx';

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
};

const UI_PARTY_GROUP_DEFINITION_TYPE = {
    TEXT: 'TEXT',
    TEXTAREA: 'TEXTAREA',
    RELATION: 'RELATION',
    RELATION_CLASS: 'RELATION-CLASS'
};

const PARTY_GENERAL_FIELDS = ['history', 'sourceInformation', 'characteristics'];
const FIELDS_BY_PARTY_TYPE_CODE = {
    [PARTY_TYPE_CODES.PERSON]: [...PARTY_GENERAL_FIELDS, ],
    [PARTY_TYPE_CODES.GROUP_PARTY]: [...PARTY_GENERAL_FIELDS, 'scope', 'foundingNorm', 'scopeNorm', 'organization'],
    [PARTY_TYPE_CODES.EVENT]: [...PARTY_GENERAL_FIELDS, ],
    [PARTY_TYPE_CODES.DYNASTY]: [...PARTY_GENERAL_FIELDS, 'genealogy'],
};


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
        const required = [];

        if (typeof values.genealogy !== "undefined") {
            required.push("genealogy")
        }
        if (typeof values.scope !== "undefined") {
            required.push("scope")
        }
        const errors = PartyDetail.requireFields(...required)(values);
        errors.creators = [];

        values.creators.forEach((i,index) => {
            if (!(i && i !== "" && typeof i == "object")) {
                errors.creators[index] = i18n('global.validation.required');
            }
        });
        if (errors.creators.length === 0) {
            delete errors.creators;
        }

        return errors;
    };

    componentDidMount() {
        this.trySetFocus();
        this.fetchIfNeeded();
        this.updateStateFromProps();
        this.props.initForm(this.handlePartyUpdate);
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        this.updateStateFromProps(nextProps);
    }

    updateStateFromProps(props = this.props, state = this.state) {

        let tmpActiveIndexes;
        if (props.partyDetail.id === this.props.partyDetail.id) {
            tmpActiveIndexes = state.activeIndexes;
        } else {
            tmpActiveIndexes = {};
        }

        let activeIndexes = tmpActiveIndexes, visibilitySettingsValue = {}, mergeIndex = {};

        if (props.userDetail && props.userDetail.settings) {
            const {settings} = props.userDetail;
            const visibilitySettings = getOneSettings(settings, SETTINGS_PARTY_PIN);

            if (visibilitySettings.value) {
                try {
                    visibilitySettingsValue = JSON.parse(visibilitySettings.value);
                    for (let key in visibilitySettingsValue) {
                        if (visibilitySettingsValue.hasOwnProperty(key) && visibilitySettingsValue[key] === false) {
                            if (this.state.visibilitySettingsValue[key] === true) {
                                mergeIndex[key] = false;
                            }
                            delete visibilitySettingsValue[key];
                        }
                    }
                } catch(e) {
                    visibilitySettingsValue = {};
                }
                activeIndexes = {
                    ...activeIndexes,
                    ...visibilitySettingsValue,
                    ...mergeIndex
                };
            } else {
                console.warn("No settings for visibility - fallback to default - closed");
            }
        }
        this.setState({activeIndexes, visibilitySettingsValue})
    }

    fetchIfNeeded = (props = this.props) => {
        const {partyDetail: {id}} = props;
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(refRecordTypesFetchIfNeeded());
        if (id) {
            this.dispatch(partyDetailFetchIfNeeded(id));
        }
    };

    trySetFocus = (props = this.props) => {
        const {_focus} = props;

        if (canSetFocus() && _focus) {
            if (isFocusFor(_focus, 'party', 2)) {
                this.setState({}, () => {
                    this.refs.partyDetail.focus();
                    //setInputFocus(this.refs.partyDetail);
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

    handleToggleActive = (identificator) => {
        this.setState({activeIndexes:{...this.state.activeIndexes, [identificator]: !this.state.activeIndexes[identificator]}});
        if (this.state.visibilitySettingsValue[identificator]) {
            this.handlePinToggle(identificator)
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
        this.dispatch(userDetailsSaveSettings(newSettings, false))
    };


    handlePartyUpdate = (party) => {
        this.dispatch(partyUpdate({
            ...this.props.partyDetail.data,
            ...party
        }));
    };

    partyAdded = (field, party) => {
        field.onChange(party);
    };

    handleAddParty = (field, partyTypeId) => {
        this.dispatch(partyAdd(partyTypeId, null, this.partyAdded.bind(this, field), false));
    };

    registerTypesToMap = (registerTypes, map, parent) => {
        if (registerTypes != null) {
            registerTypes.forEach((item) => {
                map[item.id] = [...parent];
                if (item.relationRoleTypIds != null) {
                    map[item.id] = [...map[item.id], ...item.relationRoleTypIds];
                }
                this.registerTypesToMap(item.children, map, map[item.id]);
            });
        }
    };

    getPartyId = (data) => {
        if(data.record.externalId) {
            if(data.record.externalSystem && data.record.externalSystem.name){
                return data.record.externalSystem.name + ':' + data.record.externalId;
            } else {
                return 'UNKNOWN:' + data.record.externalId;
            }
        } else  {
            return data.id;
        }
    }

    render() {
        const {userDetail, partyDetail, fields, recordTypes} = this.props;
        const {sourceInformation, creators} = fields;
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
        var type = party.partyType.code;
        var icon = PartyListItem.partyIconByPartyTypeCode(type);
        
        let canEdit = userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: party.record.scopeId});

        const partyType = this.getPartyType();

        let parts = partyType && partyType.partyGroups ? partyType.partyGroups : [];

        let relationClassTypes = partyType && partyType.relationTypes ? getMapFromList(partyType.relationTypes.map(i => i.relationClassType), "code") : [];

        const events = {onPin:this.handlePinToggle, onSelect: this.handleToggleActive};

        return <Shortcuts name='PartyDetail' handler={this.handleShortcuts}>
            <div tabIndex={0} ref='partyDetail' className="party-detail">
                <div className="party-header">
                    <div className="header-icon">
                        <Icon glyph={icon}/>
                    </div>
                    <div className="header-content">
                        <div>
                            <div>
                                <div className="title">{party.name}</div>
                            </div>
                        </div>
                        <div>
                            <div className="description">{this.getPartyId(party)}</div>
                            <div>{dateTimeToString(new Date(party.record.lastUpdate))}</div>
                        </div>
                    </div>
                </div>
                <div className="party-type">
                    {party.partyType.description}
                </div>
                <Form className="party-body">
                    {parts.map((i, index) => {
                        const TYPE = i.type.toUpperCase();
                        if (TYPE == UI_PARTY_GROUP_TYPE.IDENT) {
                            const key = UI_PARTY_GROUP_TYPE.IDENT;
                            return <div key={index}>
                                <CollapsablePanel tabIndex={0} isOpen={activeIndexes && activeIndexes[key] === true} pinned={visibilitySettingsValue && visibilitySettingsValue[key] === true} header={i.name} eventKey={key} {...events}>
                                    <PartyDetailNames party={party} partyType={partyType} onPartyUpdate={this.handlePartyUpdate} canEdit={canEdit} />
                                    {party.partyType.code == PARTY_TYPE_CODES.GROUP_PARTY && <PartyDetailIdentifiers party={party} onPartyUpdate={this.handlePartyUpdate} canEdit={canEdit} />}
                                </CollapsablePanel>
                            </div>;
                        } else if (TYPE == UI_PARTY_GROUP_TYPE.CONCLUSION) {
                            const key = UI_PARTY_GROUP_TYPE.CONCLUSION;
                            return <div key={index}>
                                <CollapsablePanel tabIndex={0} isOpen={activeIndexes && activeIndexes[key] === true} pinned={visibilitySettingsValue && visibilitySettingsValue[key] === true} header={i.name} eventKey={key} {...events}>
                                    <FormInput componentClass="textarea" {...sourceInformation} label={i18n("party.detail.sources")} />
                                    <label className="group-label">{i18n("party.detail.creators")}{canEdit && <Button bsStyle="action" onClick={() => creators.addField({})}><Icon glyph="fa-plus" /></Button>}</label>
                                    {creators.map((creator, index) => <div key={index + "-" + creator.id} className="value-group">
                                        <div className='desc-item-value desc-item-value-parts'>
                                            <PartyField onCreate={this.handleAddParty.bind(this, creator)} {...creator} />
                                            {canEdit && <Button bsStyle="action" onClick={() => {
                                                if (confirm(i18n('party.detail.creator.delete'))) {
                                                    creators.removeField(index)
                                                }
                                            }}><Icon glyph="fa-trash" /></Button>}
                                        </div>
                                    </div>)}
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

                                            const DEFINITION_TYPE = item.type.toUpperCase();

                                            if (
                                                !(
                                                    (DEFINITION_TYPE === UI_PARTY_GROUP_DEFINITION_TYPE.TEXT || DEFINITION_TYPE === UI_PARTY_GROUP_DEFINITION_TYPE.TEXTAREA) &&
                                                    FIELDS_BY_PARTY_TYPE_CODE[partyType.code].indexOf(item.definition) === -1
                                                )
                                            ) {
                                                let element = null;
                                                let registerTypesMap = {};
                                                this.registerTypesToMap(recordTypes.items, registerTypesMap, []);

                                                if (DEFINITION_TYPE === UI_PARTY_GROUP_DEFINITION_TYPE.TEXT) {
                                                    element = <FormInput {...inputProps} type="text" disabled={!canEdit} />
                                                } else if (DEFINITION_TYPE === UI_PARTY_GROUP_DEFINITION_TYPE.TEXTAREA) {
                                                    element = <FormInput {...inputProps} componentClass="textarea" disabled={!canEdit} />
                                                } else if (DEFINITION_TYPE === UI_PARTY_GROUP_DEFINITION_TYPE.RELATION) {
                                                    const type = objectById(partyType.relationTypes, item.definition, 'code');
                                                    if (type) {
                                                        element = <PartyDetailRelations party={party} relationType={type} registerTypesMap={registerTypesMap} label={item.name} canEdit={canEdit} />
                                                    } else {
                                                        element = i18n('party.detail.ui.unknownRelation')
                                                    }
                                                } else if (DEFINITION_TYPE === UI_PARTY_GROUP_DEFINITION_TYPE.RELATION_CLASS) {
                                                    if (relationClassTypes[item.definition]) {
                                                        element = <PartyDetailRelationClass party={party} partyType={partyType} registerTypesMap={registerTypesMap} relationClassType={relationClassTypes[item.definition]} label={item.name} canEdit={canEdit} />
                                                    } else {
                                                        element = i18n('party.detail.ui.unknownRelation')
                                                    }
                                                }

                                                items.push(<div key={key} className={"el-" + (item.width ? item.width : 0)}>
                                                    {element}
                                                </div>);
                                            }
                                        }
                                    }
                                } else {
                                    items.push(<span>{i18n('party.detail.ui.definitionError')}</span>)
                                }
                            }

                            const key = i.code;

                            return <CollapsablePanel tabIndex={0} key={key} isOpen={activeIndexes && activeIndexes[key] === true}
                                                     pinned={visibilitySettingsValue && visibilitySettingsValue[key] === true} header={i.name}
                                                     eventKey={key} {...events}>
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
        const {app: {partyDetail}, userDetail, refTables: {partyTypes, recordTypes}, focus} = state;
        return {
            partyDetail,
            userDetail,
            partyTypes,
            recordTypes,
            _focus: focus,
            initialValues: partyDetail.fetched ? partyDetail.data : {}
        }
    },
    {initForm: (onSave) => (initForm('partyDetail', PartyDetail.validate, onSave))}
)(PartyDetail);
