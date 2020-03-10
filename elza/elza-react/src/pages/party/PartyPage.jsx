import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, ControllableDropdownButton, i18n, Icon, RibbonGroup, Utils} from 'components/shared';
import Ribbon from '../../components/page/Ribbon';
import PartyList from '../../components/party/PartyList';
import PartyDetail from '../../components/party/PartyDetail';
import ImportForm from '../../components/form/ImportForm';
import ExtImportForm from '../../components/form/ExtImportForm';
import {Dropdown} from 'react-bootstrap';
import {Button} from '../../components/ui';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx';
import {partyAdd, partyDelete, partyDetailFetchIfNeeded, partyListInvalidate} from 'actions/party/party.jsx';
import {Shortcuts} from 'react-shortcuts';
import {setFocus} from 'actions/global/focus.jsx';
import * as perms from 'actions/user/Permission.jsx';
import defaultKeymap from './PartyPageKeymap.jsx';
import './PartyPage.scss';
import {apExtSystemListFetchIfNeeded} from 'actions/registry/apExtSystemList';
import PageLayout from '../shared/layout/PageLayout';
import {PropTypes} from 'prop-types';
import {FOCUS_KEYS} from '../../constants.tsx';
import ScopeLists from '../../components/arr/ScopeLists';
import ApStateHistoryForm from '../../components/registry/ApStateHistoryForm';
import ApStateChangeForm from '../../components/registry/ApStateChangeForm';
import {WebApi} from '../../actions';
import {partyDetailInvalidate} from '../../actions/party/party';

/**
 * PARTY PAGE
 * *********************************************
 * Stránka osob
 */
class PartyPage extends AbstractReactComponent {
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
        userDetail: PropTypes.object.isRequired,
        refTables: PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
        if (this.props.userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            this.props.dispatch(apExtSystemListFetchIfNeeded());
        }
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
        if (nextProps.userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            this.props.dispatch(apExtSystemListFetchIfNeeded());
        }
    }

    handleShortcuts = (action) => {
        console.log('#handleShortcuts', '[' + action + ']', this);
        switch (action) {
            case 'addParty':
                this.refs.addParty.setOpen(true);
                break;
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.PARTY, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.PARTY, 2));
                break;
            case 'area3':
                this.props.dispatch(setFocus(FOCUS_KEYS.PARTY, 3));
                break;
        }
    };

    /**
     * ADD PARTY
     * *********************************************
     * Uložení nové osoby
     */
    addParty = (data) => {
        this.props.dispatch(partyDetailFetchIfNeeded(data.id));
        this.props.dispatch(partyListInvalidate());
    };

    /**
     * HANDLE ADD PARTY
     * *********************************************
     * Kliknutí na tlačítko pro založení nové osoby
     * @param partyTypeId - identifikátor typu osoby (osoba, rod, korporace, ..)
     */
    handleAddParty = (partyTypeId) => {
        this.props.dispatch(partyAdd(partyTypeId, -1, this.addParty, false));
    };


    handleImport = () => {
        this.props.dispatch(modalDialogShow(this, i18n('import.title.party'), <ImportForm party/>));
    };

    handleExtImport = () => {
        this.props.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={true}
                                                                                          onSubmitForm={(data) => {
                                                                                              this.props.dispatch(partyDetailFetchIfNeeded(data.partyId));
                                                                                              this.props.dispatch(partyListInvalidate());
                                                                                          }}/>, 'dialog-lg'));
    };


    handleShowApHistory = () => {
        const {partyDetail: {data: {accessPoint: {id}}}} = this.props;
        const form = <ApStateHistoryForm accessPointId={id}/>;
        this.props.dispatch(modalDialogShow(this, i18n('ap.history.title'), form, 'dialog-lg'));
    };

    handleChangeApState = () => {
        const {partyDetail: {data: {accessPoint: {id, typeId, scopeId}, partyType}}} = this.props;
        const form = <ApStateChangeForm initialValues={{
            typeId: typeId,
            scopeId: scopeId,
        }} partyTypeId={partyType.id} onSubmit={(data) => {
            const finalData = {
                comment: data.comment,
                state: data.state,
                typeId: data.typeId,
                scopeId: data.scopeId !== '' ? parseInt(data.scopeId) : null,
            };
            return WebApi.changeState(id, finalData);
        }} onSubmitSuccess={() => {
            this.props.dispatch(modalDialogHide());
            this.props.dispatch(partyDetailInvalidate());
            this.props.dispatch(partyListInvalidate());
        }} accessPointId={id}/>;
        this.props.dispatch(modalDialogShow(this, i18n('ap.state.change'), form));
    };

    /**
     * HANDLE DELETE PARTY
     * *********************************************
     * Kliknutí na tlačítko pro smazání osoby
     */
    handleDeleteParty = () => {
        confirm(i18n('party.delete.confirm')) && this.props.dispatch(partyDelete(this.props.partyDetail.data.id));
    };

    /* MCV-45365
    handleSetValidParty = () => {
        confirm(i18n('party.setValid.confirm')) && this.props.dispatch(setValidParty(this.props.partyDetail.data.id));
    };
    */

    handleScopeManagement = () => {
        this.props.dispatch(modalDialogShow(this, i18n('accesspoint.scope.management.title'), <ScopeLists/>));
    };

    /**
     * BUILD RIBBON
     * *********************************************
     * Sestavení Ribbon Menu - přidání položek pro osoby
     */
    buildRibbon = () => {
        const {userDetail, partyDetail, refTables: {partyTypes}, extSystems, module, customRibbon} = this.props;

        const parts = module && customRibbon ? customRibbon : {altActions: [], itemActions: [], primarySection: null};

        const isSelected = partyDetail.id !== null;
        const altActions = [...parts.altActions];
        if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL)) {
            altActions.push(
                <ControllableDropdownButton key='add-party' ref='addParty' id='add-party'
                                            title={<span className="dropContent">
                   <Icon glyph='fa-plus-circle'/><div><span
                                                className="btnText">{i18n('party.addParty')}</span></div></span>}>
                    {
                        partyTypes.items.map(
                            type => <Dropdown.Item key={type.id} eventKey={type.id}
                                                   onClick={this.handleAddParty.bind(this, type.id)}>{type.name}</Dropdown.Item>,
                        )
                    }
                </ControllableDropdownButton>,
            );
            altActions.push(<Button key='import-party' onClick={this.handleImport}>
                <Icon glyph='fa-download fa-fw'/>
                <div><span className="btnText">{i18n('ribbon.action.party.import')}</span></div>
            </Button>);
            if (extSystems && extSystems.length > 0) {
                altActions.push(<Button key='import-ext-party' onClick={this.handleExtImport}>
                    <Icon glyph='fa-download fa-fw'/>
                    <div><span className="btnText">{i18n('ribbon.action.party.importExt')}</span></div>
                </Button>);
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
        if (isSelected && partyDetail.fetched && !partyDetail.isFetching) {
            if (userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {
                type: perms.AP_SCOPE_WR,
                scopeId: partyDetail.data.accessPoint.scopeId,
            })) {
                itemActions.push(
                    <Button disabled={partyDetail.data.accessPoint.invalid} key='delete-party'
                            onClick={this.handleDeleteParty}><Icon glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('party.delete.button')}</span></div>
                    </Button>,
                );

                this.props.onShowUsage && partyDetail && itemActions.push(
                    <Button key='partyShow' onClick={() => this.props.onShowUsage(partyDetail)}>
                        <Icon glyph="fa-search"/>
                        <div><span className="btnText">{i18n('party.usage.button')}</span></div>
                    </Button>,
                );

                /* MCV-45365
                 partyDetail && itemActions.push(
                     <Button disabled={ !partyDetail.data.accessPoint.invalid} key='partySetValid' onClick={ this.handleSetValidParty }>
                         <Icon glyph="fa-check"/>
                         <div><span className="btnText">{i18n("party.setValid.button")}</span></div>
                     </Button>
                 );
                 */
            }

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
            altSection = <RibbonGroup key='alt-actions' className="small">{altActions}</RibbonGroup>;
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key='item-actions' className="small">{itemActions}</RibbonGroup>;
        }

        return <Ribbon primarySection={parts.primarySection} altSection={altSection} itemSection={itemSection}/>;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */
    render() {
        const {splitter, status} = this.props;

        const leftPanel = <PartyList/>;

        const centerPanel = <PartyDetail/>;

        return <Shortcuts name='Party' handler={this.handleShortcuts} global className="main-shortcuts2"
                          alwaysFireHandler stopPropagation={false}>
            <PageLayout
                splitter={splitter}
                className='party-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                status={status}
            />
        </Shortcuts>;
    }
}


function mapStateToProps(state) {
    const {app: {partyList, partyDetail, apExtSystemList}, splitter, refTables, userDetail, focus} = state;

    return {
        extSystems: apExtSystemList.fetched ? apExtSystemList.rows : null,
        partyList,
        partyDetail,
        splitter,
        refTables,
        userDetail,
        focus,
    };
}

export default connect(mapStateToProps)(PartyPage);
