import React from 'react';
import ReactDOM from 'react-dom';

import classNames from 'classnames';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {connect} from 'react-redux'
import {AbstractReactComponent, RibbonGroup, ModalDialog, i18n, Loading, NodeTabs, Icon, Utils} from 'components/shared';
import Ribbon from 'components/page/Ribbon'
import ImportForm from 'components/form/ImportForm'
import ExtImportForm from 'components/form/ExtImportForm'
import RegistryDetail from 'components/registry/RegistryDetail'
import RegistryList from 'components/registry/RegistryList'
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {Button} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {registryMoveStart, registryMove, registryMoveCancel, registryDelete, registryDetailFetchIfNeeded, registryAdd, registryListInvalidate} from 'actions/registry/registry.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {apExtSystemListFetchIfNeeded} from 'actions/registry/apExtSystemList';
import {PropTypes} from 'prop-types';
import './RegistryPage.less';
import PageLayout from "../shared/layout/PageLayout";
import defaultKeymap from './RegistryPageKeymap.jsx';
import {FOCUS_KEYS} from "../../constants";
/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistryPage extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    static PropTypes = {
        splitter: React.PropTypes.object.isRequired,
        registryRegion: React.PropTypes.object.isRequired,
        refTables: React.PropTypes.object.isRequired,
        focus: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired
    };

    state = {items: []};

    componentDidMount() {
        this.initData();
    }

    componentWillReceiveProps(nextProps) {
        this.initData(nextProps);
    }


    canMoveRegistry = () => {
        const {registryDetail: {id, data}, registryList:{filter:{registryParentId}, recordForMove}} = this.props;

        return id &&
            data &&
            !recordForMove &&
            !data.partyId &&
            data.hierarchical &&
            id != registryParentId
    };

    canDeleteRegistry = () => {
        const {registryDetail: {id, data}, registryList:{filter:{registryParentId}}} = this.props;

        return id &&
            data &&
            id != registryParentId
    };

    canMoveApplyCancelRegistry = () => {
        const {registryDetail: {id, data}, registryList:{recordForMove}} = this.props;

        return id &&
            data &&
            recordForMove &&
            !data.partyId
    };

    initData = (props = this.props) => {
        this.dispatch(refRecordTypesFetchIfNeeded());
        if (props.userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            this.dispatch(apExtSystemListFetchIfNeeded());
        }

        this.trySetFocus(props)
    };

    trySetFocus = (props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.registryList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                       this.refs.registryList.focus();
                       focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1) || isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1, 'list')) {
                this.setState({}, () => {
                   this.refs.registryList.focus();
                   focusWasSet()
                })
            }
        }
    };

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'addRegistry':
                this.handleAddRegistry();
                break;
            case 'registryMove':
                if (this.canMoveRegistry()) {
                    this.handleRegistryMoveStart()
                }
                break;
            case 'registryMoveApply':
                if (this.canMoveApplyCancelRegistry()) {
                    this.handleRegistryMoveConfirm()
                }
                break;
            case 'registryMoveCancel':
                if (this.canMoveApplyCancelRegistry()) {
                    this.handleRegistryMoveCancel()
                }
                break;
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 2));
                break
        }
    };

    handleAddRegistry = () => {
        const {registryList: {filter:{registryParentId, versionId}, parents}} = this.props;
        let parentName = '';

        const parentIndex = indexById(parents, registryParentId);
        if (parentIndex !== null) {
            parentName = parents[parentIndex].name;
        }

        this.dispatch(registryAdd(registryParentId, versionId === null ? -1 : versionId, this.handleCallAddRegistry, parentName, false));
    };

    handleCallAddRegistry = (data) => {
        this.dispatch(registryDetailFetchIfNeeded(data.id));
        this.dispatch(registryListInvalidate());
    };

    handleDeleteRegistry = () => {
        if (confirm(i18n('registry.deleteRegistryQuestion'))) {
            const {registryDetail:{data:{id}}} = this.props;
            this.dispatch(registryDelete(id));
        }
    };

    /* MCV-45365
    handleSetValidParty = () => {
        confirm(i18n('party.setValid.confirm')) && this.dispatch(setValidRegistry(this.props.registryDetail.data.id));
    };
    */

    handleRegistryMoveStart = () => {
        const {registryDetail:{data}} = this.props;
        this.dispatch(registryMoveStart(data));
    };

    handleRegistryMoveConfirm = () => {
        const {registryList: {filter: {registryParentId}}} = this.props;
        this.dispatch(registryMove(registryParentId));
    };

    handleRegistryMoveCancel = () => {
        this.dispatch(registryMoveCancel());
    };


    handleRegistryImport = () => {
       this.dispatch(
           modalDialogShow(this,
               i18n('import.title.registry'),
               <ImportForm record/>
           )
       );
    };

    handleExtImport = () => {
        this.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={false} onSubmitForm={(data) => {
            this.dispatch(registryDetailFetchIfNeeded(data.id));
            this.dispatch(registryListInvalidate());
        }}/>, "dialog-lg"));
    };

    buildRibbon = () => {
        const {registryDetail:{data}, userDetail, extSystems, module, customRibbon, registryDetail } = this.props;

        const parts = module && customRibbon ? customRibbon : {altActions: [], itemActions: [], primarySection: null};

        const altActions = [...parts.altActions];

        if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            altActions.push(
                <Button key='addRegistry' onClick={this.handleAddRegistry}>
                    <Icon glyph="fa-download"/>
                    <div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div>
                </Button>
            );
            altActions.push(
                <Button key='registryImport' onClick={this.handleRegistryImport}>
                    <Icon glyph='fa-download'/>
                    <div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div>
                </Button>
            );
            if (extSystems && extSystems.length > 0) {
                altActions.push(
                    <Button key='registryExtImport' onClick={this.handleExtImport}>
                        <Icon glyph='fa-download'/>
                        <div><span className="btnText">{i18n('ribbon.action.registry.importExt')}</span></div>
                    </Button>
                );
            }
        }

        const itemActions = [...parts.itemActions];
        if (this.canDeleteRegistry()) {
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {type: perms.AP_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
                itemActions.push(
                    <Button disabled={data.invalid && data.partyId} key='registryRemove' onClick={this.handleDeleteRegistry}>
                        <Icon glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('registry.deleteRegistry')}</span></div>
                    </Button>
                );
            }

            this.props.onShowUsage && itemActions.push(
                <Button key='registryShow' onClick={() => this.props.onShowUsage(registryDetail)}>
                    <Icon glyph="fa-search"/>
                    <div><span className="btnText">{i18n('registry.registryUsage')}</span></div>
                </Button>
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

        if (this.canMoveRegistry()) {
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {type: perms.AP_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
                itemActions.push(
                    <Button key='registryMove' onClick={this.handleRegistryMoveStart}>
                        <Icon glyph="fa-share"/>
                        <div><span className="btnText">{i18n('registry.moveRegistry')}</span></div>
                    </Button>
                );
            }
        }
        if (this.canMoveApplyCancelRegistry()) {
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {type: perms.AP_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
                itemActions.push(
                    <Button key='registryMoveApply' onClick={this.handleRegistryMoveConfirm}>
                        <Icon glyph="fa-check-circle"/>
                        <div><span className="btnText">{i18n('registry.applyMove')}</span></div>
                    </Button>
                );
                itemActions.push(
                    <Button key='registryMoveCancel' onClick={this.handleRegistryMoveCancel}>
                        <Icon glyph="fa-times"/>
                        <div><span className="btnText">{i18n('registry.cancelMove')}</span></div>
                    </Button>
                );
            }
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="ribbon-alt-actions" className="small">{altActions}</RibbonGroup>
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="ribbon-item-actions" className="small">{itemActions}</RibbonGroup>
        }

        return <Ribbon primarySection={parts.primarySection} altSection={altSection} itemSection={itemSection} />
    };

    render() {
        const {splitter, status} = this.props;



        const centerPanel = <div className='registry-page'>
            <RegistryDetail goToPartyPerson={this.props.goToPartyPerson}/>
        </div>;

        return <Shortcuts name='Registry' handler={this.handleShortcuts} global stopPropagation={false} className="main-shortcuts2">
            <PageLayout
                splitter={splitter}
                key='registryPage'
                ribbon={this.buildRibbon()}
                leftPanel={<RegistryList />}
                centerPanel={centerPanel}
                status={status}
            />
        </Shortcuts>
    }
}


export default connect((state) => {
    const {app:{apExtSystemList, registryDetail, registryList},splitter, refTables, focus, userDetail} = state;
    return {
        extSystems: apExtSystemList.fetched ? apExtSystemList.rows : null,
        splitter,
        registryDetail,
        registryList,
        refTables,
        focus,
        userDetail
    }
})(RegistryPage);
