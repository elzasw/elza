import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {ControllableDropdownButton, Icon, AbstractReactComponent, Ribbon, RibbonGroup, PartyList, PartyDetail, PartyEntities, i18n, ImportForm} from 'components/index.jsx';
import {RelationForm, AddPartyForm, ExtImportForm} from 'components/index.jsx';
import {MenuItem, Button} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {AppStore} from 'stores/index.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyDetailFetchIfNeeded, partyListInvalidate, PARTY_LIST_MAX_SIZE, partyAdd, partyCreate, insertRelation, partyDelete} from 'actions/party/party.jsx'
import {Shortcuts} from 'react-shortcuts';
import {Utils} from 'components/index.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {SelectPage} from 'pages'

import './PartyPage.less';
import {regExtSystemListFetchIfNeeded} from 'actions/registry/regExtSystemList';

/**
 * PARTY PAGE
 * *********************************************
 * Stránka osob
 */
class PartyPage extends AbstractReactComponent {


    static PropTypes = {
        splitter: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
        refTables: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
        this.props.dispatch(regExtSystemListFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
        this.props.dispatch(regExtSystemListFetchIfNeeded());
    }

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'addParty':
                this.refs.addParty.setOpen(true);
                break;
            case 'area1':
                this.dispatch(setFocus('party', 1));
                break;
            case 'area2':
                this.dispatch(setFocus('party', 2));
                break
            case 'area3':
                this.dispatch(setFocus('party', 3));
                break;
        }
    };

    /**
     * ADD PARTY
     * *********************************************
     * Uložení nové osoby
     */
    addParty = (data) => {
        this.dispatch(partyDetailFetchIfNeeded(data.id));
        this.dispatch(partyListInvalidate());
    };

    /**
     * HANDLE ADD PARTY
     * *********************************************
     * Kliknutí na tlačítko pro založení nové osoby
     * @param partyTypeId - identifikátor typu osoby (osoba, rod, korporace, ..)
     */
    handleAddParty = (partyTypeId) => {
        this.dispatch(partyAdd(partyTypeId, -1, this.addParty, false));
    };


    handleImport = () => {
        this.dispatch(modalDialogShow(this, i18n('import.title.party'), <ImportForm party/>));
    };

    handleExtImport = () => {
        this.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={true} onSubmitForm={(data) => {
            this.dispatch(partyDetailFetchIfNeeded(data.partyId));
            this.dispatch(partyListInvalidate());
        }} />, "dialog-lg"));
    };

    /**
     * HANDLE DELETE PARTY
     * *********************************************
     * Kliknutí na tlačítko pro smazání osoby
     */
    handleDeleteParty = () => {
        confirm(i18n('party.delete.confirm')) && this.dispatch(partyDelete(this.props.partyDetail.data.id));
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
        if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL)) {
            altActions.push(
                <ControllableDropdownButton key='add-party' ref='addParty' id='add-party' title={<span className="dropContent"><Icon glyph='fa-download fa-fw' /><div><span className="btnText">{i18n('party.addParty')}</span></div></span>}>
                    {partyTypes.items.map(type => <MenuItem key={type.id} eventKey={type.id} onClick={this.handleAddParty.bind(this, type.id)}>{type.name}</MenuItem>)}
                </ControllableDropdownButton>
            );
            altActions.push(<Button key='import-party' onClick={this.handleImport}>
                <Icon glyph='fa-download fa-fw' />
                <div><span className="btnText">{i18n('ribbon.action.party.import')}</span></div>
            </Button>);
            if (extSystems && extSystems.length > 0) {
                altActions.push(<Button key='import-ext-party' onClick={this.handleExtImport}>
                    <Icon glyph='fa-download fa-fw' />
                    <div><span className="btnText">{i18n('ribbon.action.party.importExt')}</span></div>
                </Button>);
            }
        }

        const itemActions = [...parts.itemActions];
        if (isSelected && partyDetail.fetched && !partyDetail.isFetching) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: partyDetail.data.record.scopeId})) {
                itemActions.push(
                    <Button key='delete-party' onClick={this.handleDeleteParty}><Icon glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('party.delete.button')}</span></div>
                    </Button>
                );
            }
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key='alt-actions' className="small">{altActions}</RibbonGroup>
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key='item-actions' className="small">{itemActions}</RibbonGroup>
        }

        return <Ribbon primarySection={parts.primarySection} altSection={altSection} itemSection={itemSection} />;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */
    render() {
        const {splitter, status} = this.props;

        const leftPanel = <PartyList />;

        const centerPanel = <PartyDetail />;

        return <Shortcuts name='Party' handler={this.handleShortcuts} global>
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
    const {app:{partyList, partyDetail, regExtSystemList}, splitter, refTables, userDetail, focus} = state;

    return {
        extSystems: regExtSystemList.fetched ? regExtSystemList.rows : null,
        partyList,
        partyDetail,
        splitter,
        refTables,
        userDetail,
        focus
    }
}

export default connect(mapStateToProps)(PartyPage);

