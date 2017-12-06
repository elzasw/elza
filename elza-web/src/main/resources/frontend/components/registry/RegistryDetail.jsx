/**
 * Komponenta detailu rejstříku
 */
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    Icon,
    CollapsablePanel,
    NoFocusButton,
    StoreHorizontalLoader,
    Utils
} from 'components/shared';
import {Form, Button} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyUpdate} from 'actions/party/party.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {registryDetailFetchIfNeeded, registryUpdate} from 'actions/registry/registry.jsx'
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
import {Shortcuts} from 'react-shortcuts';
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {initForm} from "actions/form/inlineForm.jsx"
import {getMapFromList} from 'stores/app/utils.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'


import {routerNavigate} from 'actions/router.jsx'
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx'
import {PropTypes} from 'prop-types';
import defaultKeymap from './RegistryDetailKeymap.jsx';
import './RegistryDetail.less';
import EditRegistryForm from "./EditRegistryForm";
import RegistryDetailVariantRecords from "./RegistryDetailVariantRecords";
import RegistryDetailCoordinates from "./RegistryDetailCoordinates";
import {requestScopesIfNeeded} from "../../actions/refTables/scopesData";


class RegistryDetail extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    state = {note:null}

    componentDidMount() {
        this.trySetFocus();
        this.fetchIfNeeded().then(data => {
            if (data && data.invalid) {
                this.props.dispatch(addToastrWarning(i18n("registry.invalid.warning")));
            }
        });
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        const {registryDetail:{id, fetched, data}} = nextProps;
        if ((id !== this.props.registryDetail.id && fetched) || (!this.props.registryDetail.fetched && fetched)) {
            if (data) {
                this.setState({note: data.note});
            } else {
                this.setState({note: null});
            }
        }
    }

    fetchIfNeeded = (props = this.props) => {
        const {registryDetail: {id}} = props;
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        this.dispatch(requestScopesIfNeeded());
        if (id) {
            return this.dispatch(registryDetailFetchIfNeeded(id));
        } else {
            return Promise.resolve(null);
        }
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;
        if (canSetFocus()) {
            if (isFocusFor(focus, 'registry', 2)) {
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
        this.dispatch(partyDetailFetchIfNeeded(this.props.registryDetail.data.partyId));
        if(!this.props.goToPartyPerson){
            this.dispatch(routerNavigate('party'));
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
        this.dispatch(
            modalDialogShow(
                this,
                i18n('registry.update.title'),
                <EditRegistryForm
                    key='editRegistryForm'
                    initData={data}
                    parentRecordId={data.parentRecordId}
                    parentRegisterTypeId={data.registerTypeId}
                    onSubmitForm={this.handleRecordUpdateCall}
                />
            )
        );
    };



    handleRecordUpdateCall = (value) => {
        const {registryDetail:{data}} = this.props;

        return this.dispatch(registryUpdate({
            ...data,
            ...value
        }, () => {
            // Nastavení focus
            this.dispatch(setFocus('registry', 2))
        }));

    };

    canEdit() {
        const {userDetail, registryDetail: { data, fetched }} = this.props;

        // Pokud je načteno && není osoba
        if (!fetched || data.partyId) {
            return false
        }

        // Pokud nemá oprávnění, zakážeme editaci
        return userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {
            type: perms.REG_SCOPE_WR,
            scopeId: data ? data.scopeId : null
        });
    }

    handleNoteBlur = (event, element) => {
        if (event.target.value !== this.props.registryDetail.data.note) {
            this.dispatch(registryUpdate({
                ...this.props.registryDetail.data,
                note: event.target.value
            }));
        }
    };

    handleNoteChange = (e) => {
        this.setState({
            note: e.target.value
        });
    };

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
    getHierarchy = (data) => {
        const hierarchy = [];
        data.typesToRoot.slice().reverse().map((val) => {
            hierarchy.push(val.name);
        });
        data.parents.slice().reverse().map((val) => {
            hierarchy.push(val.name);
        });
        return hierarchy;
    }

    createHierarchyElement = (hierarchy,delimiter = ">") => {
        var hierarchyElement = [];
        for(var i = 0; i < hierarchy.length; i++){
            if(i > 0){hierarchyElement.push(<span key="hierarchy-delimiter" className="hierarchy-delimiter">{delimiter}</span>);}
            hierarchyElement.push(<span key={i + "hierarchy-level"} className="hierarchy-level">{hierarchy[i].toUpperCase()}</span>);
        }
        return hierarchyElement;
    }

    getScopeLabel = (scopeId, scopes) => {
        return scopeId && scopes[0].scopes.find(scope => (scope.id === scopeId)).name.toUpperCase();
    };

    render() {
        const {registryDetail, scopes} = this.props;
        const {data, fetched, isFetching, id} = registryDetail;

        let icon = 'fa-folder';

        if (registryDetail.data && registryDetail.data.hierarchical === false) {
            icon = 'fa-file-o';
        }

        if (!id) {
            return <div className="unselected-msg">
                <div className="title">{i18n('registry.noSelection.title')}</div>
                <div className="msg-text">{i18n('registry.noSelection.message')}</div>
            </div>;
        }

        if (!fetched || (id && !data)) {
            return <StoreHorizontalLoader store={registryDetail}/>
        }

        const disableEdit = !this.canEdit();


        var hierarchy = this.getHierarchy(data);
        var delimiter = <Icon glyph="fa-angle-right"/>;
        var hierarchyElement = this.createHierarchyElement(hierarchy,delimiter);

        let headerCls = "registry-header";
        if (data.invalid) {
            headerCls += " invalid";
        }

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
                                    <NoFocusButton disabled={disableEdit} className="registry-record-edit btn-action" onClick={this.handleRecordUpdate}>
                                        <Icon glyph='fa-pencil'/>
                                    </NoFocusButton>
                                    {data.partyId && <NoFocusButton className="registry-record-party btn-action" onClick={this.handleGoToParty}>
                                        <Icon glyph='fa-user'/>
                                    </NoFocusButton>}
                                </div>
                            </div>
                            <div>
                                <div className="description">{this.getRecordId(data)}</div>
                                <div>{dateTimeToString(new Date(data.lastUpdate))}</div>
                            </div>
                        </div>
                    </div>
                    <div className="registry-type">
                        {hierarchyElement}
                        {data.scopeId && <span className="scope-label">
                            {scopes && this.getScopeLabel(data.scopeId, scopes)}
                        </span>}
                    </div>
                    <div ref='registryTitle' className="registry-title" tabIndex={0}>
                        <div className='registry-content'>

                            <div className='line charakteristik'>
                                <label>{i18n('registry.detail.characteristics')}</label>
                                <div>{data.characteristics}</div>
                            </div>

                            <div className='line variant-name'>
                                <label>{i18n('registry.detail.variantRegistry')}</label>
                                <RegistryDetailVariantRecords value={data.variantRecords ? data.variantRecords : []} regRecordId={data.id} disabled={disableEdit} />
                            </div>
                            <div className='line'>
                                <div className='reg-coordinates-labels'>
                                    <label>{i18n('registry.detail.coordinates')}</label>
                                    <label>{i18n('registry.coordinates.description')}</label>
                                </div>
                                <RegistryDetailCoordinates value={data.coordinates ? data.coordinates : []} regRecordId={data.id} disabled={disableEdit} />
                            </div>
                            <div className='line note'>
                                <label>{i18n('registry.detail.note')}</label>
                                <FormInput disabled={disableEdit} onChange={this.handleNoteChange} onBlur={this.handleNoteBlur} componentClass='textarea' value={this.state.note} />
                            </div>
                        </div>
                    </div>
                </div>
            </Shortcuts>
        </div>
    }
}

export default connect((state) => {
    const {app: {registryDetail}, userDetail, focus, refTables} = state;
    return {
        focus,
        registryDetail,
        userDetail,
        scopes: refTables.scopesData.scopes
    }
})(RegistryDetail);
