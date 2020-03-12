import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon, RibbonGroup, Utils} from '../../components/shared';
import Ribbon from '../../components/page/Ribbon';
import ImportForm from '../../components/form/ImportForm';
import ExtImportForm from '../../components/form/ExtImportForm';
import RegistryDetail from '../../components/registry/RegistryDetail';
import RegistryList from '../../components/registry/RegistryList';
import {Button} from '../../components/ui';
import {
    apMigrate,
    registryAdd,
    registryDelete,
    registryDetailFetchIfNeeded,
    registryListInvalidate,
} from '../../actions/registry/registry.jsx';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog.jsx';
import {refRecordTypesFetchIfNeeded} from '../../actions/refTables/recordTypes.jsx';
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from '../../actions/global/focus.jsx';
import * as perms from '../../actions/user/Permission.jsx';
import {apExtSystemListFetchIfNeeded} from '../../actions/registry/apExtSystemList';
import {PropTypes} from 'prop-types';
import './RegistryPage.scss';
import PageLayout from '../shared/layout/PageLayout';
import defaultKeymap from './RegistryPageKeymap.jsx';
import {FOCUS_KEYS} from '../../constants.tsx';
import * as eidTypes from '../../actions/refTables/eidTypes';
import ScopeLists from '../../components/arr/ScopeLists';
import ApStateHistoryForm from '../../components/registry/ApStateHistoryForm';
import ApStateChangeForm from '../../components/registry/ApStateChangeForm';
import {registryDetailInvalidate} from '../../actions/registry/registry';
import {WebApi} from '../../actions';

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistryPage extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    static propTypes = {
        splitter: PropTypes.object.isRequired,
        registryRegion: PropTypes.object.isRequired,
        refTables: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
    };

    state = {items: []};

    componentDidMount() {
        this.initData();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.initData(nextProps);
    }

    canMigrateAp = () => {
        const {registryDetail: {id, data}, refTables} = this.props;
        const apTypeIdMap = refTables.recordTypes.typeIdMap;

        return id &&
            data &&
            apTypeIdMap[data.typeId].ruleSystemId !== null &&
            data.ruleSystemId === null;
    };

    canDeleteRegistry = () => {
        // We can delete item if has id and data
        const {id, data} = this.props.registryDetail;

        return id && data;
    };

    initData = (props = this.props) => {
        props.dispatch(refRecordTypesFetchIfNeeded());
        props.dispatch(eidTypes.fetchIfNeeded());
        if (props.userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            props.dispatch(apExtSystemListFetchIfNeeded());
        }

        this.trySetFocus(props);
    };

    trySetFocus = (props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.registryList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.registryList.focus();
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1) || isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1, 'list')) {
                this.setState({}, () => {
                    this.refs.registryList.focus();
                    focusWasSet();
                });
            }
        }
    };

    handleShortcuts = (action) => {
        console.log('#handleShortcuts', '[' + action + ']', this);
        switch (action) {
            case 'addRegistry':
                this.handleAddRegistry();
                break;
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 2));
                break;
            default:
                break;
        }
    };

    handleAddRegistry = () => {
        const {registryList: {filter: {versionId}, parents}} = this.props;

        this.props.dispatch(registryAdd(versionId === null ? -1 : versionId, this.handleCallAddRegistry, false));
    };

    handleCallAddRegistry = (data) => {
        this.props.dispatch(registryDetailFetchIfNeeded(data.id));
        this.props.dispatch(registryListInvalidate());
    };

    handleDeleteRegistry = () => {
        if (window.confirm(i18n('registry.deleteRegistryQuestion'))) {
            const {registryDetail: {data: {id}}} = this.props;
            this.props.dispatch(registryDelete(id));
        }
    };

    handleApMigrate = () => {
        const {registryDetail: {data: {id}}} = this.props;
        this.props.dispatch(apMigrate(id));
    };

    /* MCV-45365
    handleSetValidParty = () => {
        confirm(i18n('party.setValid.confirm')) && this.props.dispatch(setValidRegistry(this.props.registryDetail.data.id));
    };
    */

    handleRegistryImport = () => {
        this.props.dispatch(
            modalDialogShow(this,
                i18n('import.title.registry'),
                <ImportForm record/>,
            ),
        );
    };

    handleExtImport = () => {
        this.props.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={false}
                                                                                          onSubmitForm={(data) => {
                                                                                              this.props.dispatch(registryDetailFetchIfNeeded(data.id));
                                                                                              this.props.dispatch(registryListInvalidate());
                                                                                          }}/>, 'dialog-lg'));
    };

    handleScopeManagement = () => {
        this.props.dispatch(modalDialogShow(this, i18n('accesspoint.scope.management.title'), <ScopeLists/>));
    };

    handleShowApHistory = () => {
        const {registryDetail: {data: {id}}} = this.props;
        const form = <ApStateHistoryForm accessPointId={id}/>;
        this.props.dispatch(modalDialogShow(this, i18n('ap.history.title'), form, 'dialog-lg'));
    };

    handleChangeApState = () => {
        const {registryDetail: {data: {id, partyId, typeId, scopeId}}} = this.props;
        const form = <ApStateChangeForm initialValues={{
            typeId: partyId === null ? typeId : null,
            scopeId: scopeId,
        }} hideType={partyId !== null} onSubmit={(data) => {
            const finalData = {
                comment: data.comment,
                state: data.state,
                typeId: data.typeId,
                scopeId: data.scopeId !== '' ? parseInt(data.scopeId) : null,
            };
            return WebApi.changeState(id, finalData);
        }} onSubmitSuccess={() => {
            this.props.dispatch(modalDialogHide());
            this.props.dispatch(registryDetailInvalidate());
            this.props.dispatch(registryListInvalidate());
        }} accessPointId={id}/>;
        this.props.dispatch(modalDialogShow(this, i18n('ap.state.change'), form));
    };

    buildRibbon = () => {
        const {registryDetail: {data, id}, userDetail, extSystems, module, customRibbon, registryDetail} = this.props;

        const parts = module && customRibbon ? customRibbon : {altActions: [], itemActions: [], primarySection: null};

        const altActions = [...parts.altActions];

        if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            altActions.push(
                <Button key='addRegistry' onClick={this.handleAddRegistry}>
                    <Icon glyph="fa-plus-circle"/>
                    <div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div>
                </Button>,
            );
            altActions.push(
                <Button key='registryImport' onClick={this.handleRegistryImport}>
                    <Icon glyph='fa-download'/>
                    <div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div>
                </Button>,
            );
            if (extSystems && extSystems.length > 0) {
                altActions.push(
                    <Button key='registryExtImport' onClick={this.handleExtImport}>
                        <Icon glyph='fa-download'/>
                        <div><span className="btnText">{i18n('ribbon.action.registry.importExt')}</span></div>
                    </Button>,
                );
            }
        }
        if (userDetail.hasOne(perms.FUND_ADMIN, perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR)) {
            altActions.push(
                <Button key='scopeManagement' onClick={this.handleScopeManagement}>
                    <Icon glyph='fa-wrench'/>
                    <div><span className="btnText">{i18n('ribbon.action.registry.scope.manage')}</span></div>
                </Button>,
            );
        }

        const itemActions = [...parts.itemActions];
        if (this.canDeleteRegistry()) {
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                type: perms.AP_SCOPE_WR,
                scopeId: data ? data.scopeId : null,
            })) {
                itemActions.push(
                    <Button disabled={data.invalid && data.partyId} key='registryRemove'
                            onClick={this.handleDeleteRegistry}>
                        <Icon glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('registry.deleteRegistry')}</span></div>
                    </Button>,
                );
            }

            this.props.onShowUsage && itemActions.push(
                <Button key='registryShow' onClick={() => this.props.onShowUsage(registryDetail)}>
                    <Icon glyph="fa-search"/>
                    <div><span className="btnText">{i18n('registry.registryUsage')}</span></div>
                </Button>,
            );

            /*
            MCV-45365
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {type: perms.AP_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
                itemActions.push(
                    <Button disabled={!data.invalid && data.partyId} key='registrySetValid'
                            onClick={() => this.handleSetValidParty()}>
                        <Icon glyph="fa-check"/>
                        <div><span className="btnText">{i18n('registry.setValid')}</span></div>
                    </Button>
                );
            }*/
        }

        if (this.canMigrateAp()) {
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                type: perms.AP_SCOPE_WR,
                scopeId: data ? data.scopeId : null,
            })) {
                itemActions.push(
                    <Button key='apMigrate' onClick={this.handleApMigrate}>
                        <Icon glyph="fa-share"/>
                        <div><span className="btnText">{i18n('registry.migrateAp')}</span></div>
                    </Button>,
                );
            }
        }

        if (id && data) {
            itemActions.push(
                <Button key='show-state-history' onClick={this.handleShowApHistory}>
                    <Icon glyph="fa-clock-o"/>
                    <div><span className="btnText">{i18n('ap.stateHistory')}</span></div>
                </Button>,
            );

            // TODO: oprávnění
            itemActions.push(
                <Button key='change-state' onClick={this.handleChangeApState}>
                    <Icon glyph="fa-pencil"/>
                    <div><span className="btnText">{i18n('ap.changeState')}</span></div>
                </Button>,
            );
        }


        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="ribbon-alt-actions" className="small">{altActions}</RibbonGroup>;
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="ribbon-item-actions" className="small">{itemActions}</RibbonGroup>;
        }

        return <Ribbon primarySection={parts.primarySection} altSection={altSection} itemSection={itemSection}/>;
    };

    render() {
        const {splitter, status} = this.props;


        const centerPanel = <div className='registry-page'>
            <RegistryDetail goToPartyPerson={this.props.goToPartyPerson}/>
        </div>;

        return <Shortcuts name='Registry' handler={this.handleShortcuts} global stopPropagation={false}
                          className="main-shortcuts2">
            <PageLayout
                splitter={splitter}
                key='registryPage'
                ribbon={this.buildRibbon()}
                leftPanel={<RegistryList/>}
                centerPanel={centerPanel}
                status={status}
            />
        </Shortcuts>;
    }
}


export default connect((state) => {
    const {app: {apExtSystemList, registryDetail, registryList}, splitter, refTables, focus, userDetail} = state;
    return {
        extSystems: apExtSystemList.fetched ? apExtSystemList.rows : null,
        splitter,
        registryDetail,
        registryList,
        refTables,
        focus,
        userDetail,
    };
})(RegistryPage);
