/**
 * Komponenta detailu rejstříku
 */
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    i18n,
    Icon,
    CollapsablePanel,
    NoFocusButton,
    StoreHorizontalLoader,
    Utils
} from '../../components/shared';
import {Form, Button} from 'react-bootstrap';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyUpdate} from 'actions/party/party.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {registryDetailFetchIfNeeded, registryUpdate, registryDetailInvalidate} from '../../actions/registry/registry.jsx'
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
import {Shortcuts} from 'react-shortcuts';
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {initForm} from "actions/form/inlineForm.jsx"
import {getMapFromList} from 'stores/app/utils.jsx'
import {setFocus} from 'actions/global/focus.jsx'

import {routerNavigate} from 'actions/router.jsx'
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {PropTypes} from 'prop-types';
import defaultKeymap from './RegistryDetailKeymap.jsx';
import './RegistryDetail.less';
import EditRegistryForm from "./EditRegistryForm";
import {requestScopesIfNeeded} from "../../actions/refTables/scopesData";
import {FOCUS_KEYS, ApState} from "../../constants.tsx";
import ApChangeDescriptionForm from "./ApChangeDescriptionForm";
import ApDetailNames from './ApDetailNames.jsx'
import {WebApi} from "../../actions/WebApi";
import AccessPointForm from "../accesspoint/AccessPointForm";
import {refRulDataTypesFetchIfNeeded} from "../../actions/refTables/rulDataTypes";
import {descItemTypesFetchIfNeeded} from "../../actions/refTables/descItemTypes";
import AddDescItemTypeForm from "../arr/nodeForm/AddDescItemTypeForm";
import {accessPointFormActions} from "../accesspoint/AccessPointFormActions";
import TooltipTrigger from "../shared/tooltip/TooltipTrigger";
import {structureTypesFetchIfNeeded} from "../../actions/refTables/structureTypes";

class RegistryDetail extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };

    componentWillMount() {
        Utils.addShortcutManager(this,defaultKeymap);
    }

    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    state = {
        activeIndexes: {"NAMES": true, "DESCRIPTION": true},
    };

    componentDidMount() {
        this.trySetFocus();
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        const {registryDetail:{id, fetched, data}} = nextProps;
        if ((id !== this.props.registryDetail.id && fetched) || (!this.props.registryDetail.fetched && fetched)) {
            if (data) {
                this.setState({});
            }
        }
    }

    fetchIfNeeded = (props = this.props) => {
        const {registryDetail: {id, fetched}, dispatch} = props;
        dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        dispatch(descItemTypesFetchIfNeeded());
        dispatch(refRulDataTypesFetchIfNeeded());
        dispatch(requestScopesIfNeeded());
        dispatch(structureTypesFetchIfNeeded());
        if (id) {
            dispatch(registryDetailFetchIfNeeded(id));
            if (fetched) {
                dispatch(accessPointFormActions.fundSubNodeFormFetchIfNeeded({id}))
            }
        }
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;
        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 2)) {
                this.setState({}, () => {
                    if (this.refs.registryTitle) {
                        this.refs.registryTitle.focus();
                        focusWasSet()
                    }
                })
            }
        }
    };

    handleGoToParty = () => {
        this.props.dispatch(partyDetailFetchIfNeeded(this.props.registryDetail.data.partyId));
        if (!this.props.goToPartyPerson) {
            this.props.dispatch(routerNavigate('party'));
        } else {
            this.props.goToPartyPerson();
        }
    };

    handleShortcuts = (action)  => {
        switch (action) {
            case 'editRecord':
                if (this.canEdit()) {
                    this.handleRecordUpdate()
                }
                break;
            case 'goToPartyPerson':
                if (this.props.registryDetail.data.partyId) {
                    this.handleGoToParty()
                }
                break;
        }
    };

    handleRecordUpdate = () => {
        const {registryDetail:{data}} = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('registry.update.title'),
                <EditRegistryForm
                    key='editRegistryForm'
                    initData={data}
                    parentApTypeId={data.apTypeId}
                    onSubmitForm={this.handleRecordUpdateCall}
                />
            )
        );
    };



    handleRecordUpdateCall = (value) => {
        const {registryDetail:{data}} = this.props;

        return this.props.dispatch(registryUpdate(data.id, value.typeId, () => {
            // Nastavení focus
            this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 2))
        }));

    };

    canEdit() {
        const {userDetail, registryDetail: { data, fetched }, apTypeIdMap} = this.props;

        // Pokud je načteno && není osoba
        if (!fetched || data.partyId) {
            return false
        }

        const type = apTypeIdMap[data.typeId];

        if (type.ruleSystemId !== data.ruleSystemId) {
            return false;
        }

        // Pokud nemá oprávnění, zakážeme editaci
        return userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
            type: perms.AP_SCOPE_WR,
            scopeId: data ? data.scopeId : null
        });
    }
    getRecordId = (data) => {
        if(data.externalId) {
            if(data.externalSystem && data.externalSystem.name){
                return data.externalSystem.name + ':' + data.externalId;
            } else {
                return 'UNKNOWN:' + data.externalId;
            }
        } else  {
            return data.id;
        }
    }

	getApId = (ap) => {
        const {eidTypes} = this.props;
		const eids = ap.externalIds;
		if (!eids || eids.length == 0) {
			return ap.id;
		}

		let eidArr = [];
		eids.forEach(eid => {
			const typeId = eid.typeId;
            const eidTypeName = eidTypes && eidTypes[typeId] ? eidTypes[typeId].name : "eid_type_name-" + typeId;
			eidArr.push(eidTypeName + ":" + eid.value);
		});
		return eidArr.join(", ");
    };

    renderApTypeNames = (apTypeId, delimiter) => {
		const type = this.props.apTypeIdMap[apTypeId];
        let elements = [];

        if (type.parents) {
			type.parents.reverse().forEach((name, i) => {
				elements.push(<span key={"name-" + i} className="hierarchy-level">{name.toUpperCase()}</span>);
				elements.push(<span key={"delimiter-" + i} className="hierarchy-delimiter">{delimiter}</span>);
			})
		}
		elements.push(<span key="name-main" className="hierarchy-level main">{type.name.toUpperCase()}</span>);

		return elements;
    };

    getScopeLabel = (scopeId, scopes) => {
        return scopeId && scopes[0].scopes.find(scope => (scope.id === scopeId)).name.toUpperCase();
    };

    handleToggleActive = (identificator) => {
        this.setState({activeIndexes:{...this.state.activeIndexes, [identificator]: !this.state.activeIndexes[identificator]}});
    };

    refreshData = () => {
        this.props.dispatch(registryDetailInvalidate());
    };

    editDescription = () => {
        const {registryDetail:{data}} = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('accesspoint.update.description'),
                <ApChangeDescriptionForm
                    initialValues={{description: data.characteristics}}
                    onSubmit={(result) => {
                        return WebApi.changeDescription(data.id, result).then(() => {
                            this.props.dispatch(registryDetailInvalidate());
                            this.props.dispatch(modalDialogHide())
                        });
                    }}
                />
            )
        );
    };
    add = ( ) => {
        const {ap} = this.props;
        const subNodeForm = ap.form;


        const formData = subNodeForm.formData;
        const itemTypes = [];
        const strictMode = true;

        let infoTypesMap = new Map(subNodeForm.infoTypesMap);

        formData.itemTypes.forEach(descItemType => {
            infoTypesMap.delete(descItemType.id);
        });

        subNodeForm.refTypesMap.forEach(refType => {
             if (infoTypesMap.has(refType.id)) {    // ještě ji na formuláři nemáme
                 const infoType = infoTypesMap.get(refType.id);
                // v nestriktním modu přidáváme všechny jinak jen možné
                if (!strictMode || infoType.type !== 'IMPOSSIBLE') {
                    // nový item type na základě původního z refTables
                    itemTypes.push(refType);
                }
            }
        });

        const descItemTypes = [
            {
                groupItem: true,
                id: "DEFAULT",
                name: i18n("subNodeForm.descItemGroup.default"),
                children: itemTypes
            }
        ];

        const submit = (data) => {
            this.props.dispatch(modalDialogHide());
            this.props.dispatch(accessPointFormActions.fundSubNodeFormDescItemTypeAdd(data.descItemTypeId.id));
        };

        // Modální dialog
        this.props.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>));
    };

    renderActions = () => {
        return <div className="form-actions-container">
            <div className="form-actions">
                <div className="section">
                    <NoFocusButton onClick={this.add}><Icon glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}</NoFocusButton>
                </div>
            </div>
        </div>
    };

    renderTooltip = (icon, content) => {
        return <TooltipTrigger
            content={content}
            holdOnHover
            placement="auto"
            tooltipClass="error-message"
            className="status btn"
            showDelay={50}
            hideDelay={0}
        >
            <Icon glyph={icon} />
        </TooltipTrigger>
    };

    renderApError = (state, errorDescription) => {
        if (state === ApState.ERROR) {
            const error = JSON.parse(errorDescription) || {};
            let content;
            if (error.scriptFail) {
                content = <div>{i18n("ap.error.script")}</div>
            }
            return this.renderTooltip("fa-exclamation-circle", content);
        }
    };

    renderApItemsError = (data) => {
        const {ap} = this.props;
        const subNodeForm = ap.form;
        const {state, errorDescription} = data;

        if (state === ApState.ERROR) {
            const error = JSON.parse(errorDescription) || {};
            let content = [];

            if(error.emptyValue){
                content.push(<div className="error-item">{i18n("ap.error.emptyValue")}</div>);
            }
            if(error.duplicateValue){
                content.push(<div className="error-item">{i18n("ap.error.duplicateValue")}</div>);
            }
            if(error.impossibleItemTypeIds && error.impossibleItemTypeIds.length > 0){
                const items = [];
                error.impossibleItemTypeIds.map((id)=>{
                    const type = subNodeForm.refTypesMap.get(id);
                    items.push(<li>{type.name}</li>);
                });
                content.push(
                    <div className="error-list error-item">
                        <div>{i18n("ap.error.impossibleItemTypeIds")}</div>
                        <ul>{items}</ul>
                    </div>
                );
            }
            if(error.requiredItemTypeIds && error.requiredItemTypeIds.length > 0){
                const items = [];
                error.requiredItemTypeIds.map((id)=>{
                    const type = subNodeForm.refTypesMap.get(id);
                    items.push(<li>{type.name}</li>);
                });
                content.push(
                    <div className="error-list error-item">
                        <div>{i18n("ap.error.requiredItemTypeIds")}</div>
                        <ul>{items}</ul>
                    </div>
                );
            }

            if (content.length > 0) {
                return <span className={"pull-right"}>{this.renderTooltip("fa-exclamation-circle", <div>{content}</div>)}</span>
            }
        }
    };

    renderApNameItemsError = (data) => {
        const {ap, refTables} = this.props;

        const itemTypes = refTables.descItemTypes.items;
        if (!refTables.descItemTypes.fetched) {
            return;
        }

        const {state, errorDescription} = data;

        if (state === ApState.ERROR) {
            const error = JSON.parse(errorDescription) || {};
            let content = [];

            if(error.emptyValue){
                content.push(<div className="error-item">{i18n("ap.error.emptyValue")}</div>);
            }
            if(error.duplicateValue){
                content.push(<div className="error-item">{i18n("ap.error.duplicateValue")}</div>);
            }
            if(error.impossibleItemTypeIds && error.impossibleItemTypeIds.length > 0){
                const items = [];
                error.impossibleItemTypeIds.map((id)=>{
                    const type = objectById(itemTypes, id);
                    items.push(<li>{type.name}</li>);
                });
                content.push(
                    <div className="error-list error-item">
                        <div>{i18n("ap.error.impossibleItemTypeIds")}</div>
                        <ul>{items}</ul>
                    </div>
                );
            }
            if(error.requiredItemTypeIds && error.requiredItemTypeIds.length > 0){
                const items = [];
                error.requiredItemTypeIds.map((id)=>{
                    const type = objectById(itemTypes, id);
                    items.push(<li>{type.name}</li>);
                });
                content.push(
                    <div className="error-list error-item">
                        <div>{i18n("ap.error.requiredItemTypeIds")}</div>
                        <ul>{items}</ul>
                    </div>
                );
            }

            if (content.length > 0) {
                return <span>{this.renderTooltip("fa-exclamation-circle", <div>{content}</div>)}</span>
            }
        }
    };

    renderApNamesError = (names) => {

        let showError = false;
        for (let i = 0; i < names.length; i++) {
            const name = names[i];
            if (name.state === ApState.ERROR) {
                showError = true;
                break;
            }
        }

        if (showError) {
            return <span className={"pull-right"}>{this.renderTooltip("fa-exclamation-circle", null)}</span>
        }
    };

    showForm = () => {
        const {registryDetail, ap, apTypeIdMap} = this.props;
        const {data} = registryDetail;
        return apTypeIdMap[data.typeId].ruleSystemId && ap.form.fetched;
    };

    render() {
        const {registryDetail, scopes, eidTypes, ap, refTables, apTypeIdMap} = this.props;
        const {data, fetched, isFetching, id} = registryDetail;
        const {activeIndexes} = this.state;

        let icon = 'fa-folder';

        if (data) {
            icon = 'fa-file-o';
        }

        if (!id) {
            return <div className="unselected-msg">
                <div className="title">{i18n('registry.noSelection.title')}</div>
                <div className="msg-text">{i18n('registry.noSelection.message')}</div>
            </div>;
        }

        if (!fetched || (id && !data) || eidTypes == null) {
            return <StoreHorizontalLoader store={registryDetail}/>
        }

        const disableEdit = !this.canEdit();

        let headerCls = "registry-header";
        if (data.invalid) {
            headerCls += " invalid";
        }

        const delimiter = <Icon glyph="fa-angle-right"/>;
		const apTypeNames = this.renderApTypeNames(data.typeId, delimiter);

        return <div className='registry'>
            <Shortcuts name='RegistryDetail' handler={this.handleShortcuts} global>
                <div className="registry-detail">
                    <div className={headerCls}>
                        <div className="header-icon">
                            <Icon glyph={icon}/>
                        </div>
                        <div className={"header-content"}>
                            <div>
                                <div>
                                    <div className="title">{data.record} {data.invalid && "(Neplatné)"}</div>
                                </div>
                                <div>
                                    {this.renderApError(data.state, data.errorDescription)}
                                    <NoFocusButton disabled={disableEdit} className="registry-record-edit btn-action" onClick={this.handleRecordUpdate}>
                                        <Icon glyph='fa-pencil'/>
                                    </NoFocusButton>
                                    {data.partyId && <NoFocusButton className="registry-record-party btn-action" onClick={this.handleGoToParty}>
                                        <Icon glyph='fa-user'/>
                                    </NoFocusButton>}
                                </div>
                            </div>
                            <div>
                                <div className="description">{this.getApId(data)}</div>
                            </div>
                        </div>
                    </div>
                    <div className="registry-type">
						{apTypeNames}
                        {data.scopeId && <span className="scope-label">
                            {scopes && this.getScopeLabel(data.scopeId, scopes)}
                        </span>}
                    </div>
                    <CollapsablePanel tabIndex={0} key={"NAMES"} isOpen={activeIndexes && activeIndexes["NAMES"] === true} header={<div>{i18n("accesspoint.detail.formNames")}{this.renderApNamesError(data.names)}</div>} eventKey={"NAMES"} onPin={this.handlePinToggle} onSelect={this.handleToggleActive}>
                        <div className={"cp-15"}>
                            <ApDetailNames accessPoint={data} type={apTypeIdMap[data.typeId]} canEdit={!disableEdit} refreshParty={this.refreshData} renderError={this.renderApNameItemsError}  />
                        </div>
                    </CollapsablePanel>
                    <CollapsablePanel tabIndex={0} key={"DESCRIPTION"} isOpen={activeIndexes && activeIndexes["DESCRIPTION"] === true} header={<div>{i18n("accesspoint.detail.description")}{this.renderApItemsError(data)}</div>} eventKey={"DESCRIPTION"} onPin={this.handlePinToggle} onSelect={this.handleToggleActive}>
                        {this.showForm() && !disableEdit && this.renderActions()}
                        <div className={"cp-15"}>
                            <div className="elements-container">
                                <div className={"el-12"}>
                                    <label>{i18n('registry.detail.characteristics')} {data.ruleSystemId == null && !disableEdit && <Button onClick={this.editDescription}><Icon glyph="fa-pencil" /></Button>}</label>
                                    <div>{data.characteristics}</div>
                                </div>
                            </div>
                            {this.showForm() && <AccessPointForm
                                versionId={null}
                                fundId={null}
                                selectedSubNodeId={ap.id}
                                rulDataTypes={refTables.rulDataTypes.items}
                                calendarTypes={refTables.calendarTypes.items}
                                descItemTypes={refTables.descItemTypes.items}
                                structureTypes={objectById(refTables.structureTypes.data, null, 'versionId')}
                                subNodeForm={ap.form}
                                closed={false}
                                focus={null}
                                readMode={disableEdit}
                            />}
                        </div>
                    </CollapsablePanel>
                </div>
            </Shortcuts>
        </div>
    }
}

export default connect((state) => {
    const {app: {registryDetail}, userDetail, focus, refTables, ap} = state;
    return {
        ap,
        focus,
        registryDetail,
        userDetail,
        scopes: refTables.scopesData.scopes,
		apTypeIdMap: refTables.recordTypes.typeIdMap,
		eidTypes: refTables.eidTypes.data,
        refTables
    }
})(RegistryDetail);
