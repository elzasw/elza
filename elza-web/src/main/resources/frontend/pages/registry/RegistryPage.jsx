import React from 'react';
import ReactDOM from 'react-dom';

import classNames from 'classnames';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components/index.jsx';
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, ArrPanel,
        SearchWithGoto, AddRegistryForm, ImportForm,
        ListBox, Autocomplete, ExtImportForm, RegistryDetail} from 'components';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {Button} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {registryMoveStart, registryMove, registryMoveCancel, registryDelete, registryDetailFetchIfNeeded, registryAdd, registryListInvalidate} from 'actions/registry/registry.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
import {Shortcuts} from 'react-shortcuts';
import {Utils, RegistryList} from 'components/index.jsx';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {regExtSystemListFetchIfNeeded} from 'actions/registry/regExtSystemList';

import './RegistryPage.less';
import {SelectPage} from 'pages'

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistryPage extends AbstractReactComponent {

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
            data.childs &&
            data.childs.length === 0 &&
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
        this.dispatch(regExtSystemListFetchIfNeeded());

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
            } else if (isFocusFor(focus, 'registry', 1) || isFocusFor(focus, 'registry', 1, 'list')) {
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
                this.dispatch(setFocus('registry', 1));
                break;
            case 'area2':
                this.dispatch(setFocus('registry', 2));
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
        const {registryDetail:{data}, userDetail, extSystems, module, customRibbon} = this.props;

        const parts = module && customRibbon ? customRibbon : {altActions: [], itemActions: [], primarySection: null};

        const altActions = [...parts.altActions];

        if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL)) {
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
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
                itemActions.push(
                    <Button key='registryRemove' onClick={this.handleDeleteRegistry}>
                        <Icon glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('registry.deleteRegistry')}</span></div>
                    </Button>
                );
            }
        }
        if (this.canMoveRegistry()) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
                itemActions.push(
                    <Button key='registryMove' onClick={this.handleRegistryMoveStart}>
                        <Icon glyph="fa-share"/>
                        <div><span className="btnText">{i18n('registry.moveRegistry')}</span></div>
                    </Button>
                );
            }
        }
        if (this.canMoveApplyCancelRegistry()) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: data ? data.scopeId : null})) {
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
            <RegistryDetail />
        </div>;

        return <Shortcuts name='Registry' handler={this.handleShortcuts} global stopPropagation={false}>
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
    const {app:{regExtSystemList, registryDetail, registryList},splitter, refTables, focus, userDetail} = state;
    return {
        extSystems: regExtSystemList.fetched ? regExtSystemList.rows : null,
        splitter,
        registryDetail,
        registryList,
        refTables,
        focus,
        userDetail
    }
})(RegistryPage);
