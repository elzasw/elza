import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {ControllableDropdownButton, Icon, AbstractReactComponent, Ribbon, RibbonGroup, PartyList, PartyDetail, PartyEntities, i18n, ImportForm} from 'components/index.jsx';
import {RelationForm, AddPartyForm} from 'components/index.jsx';
import {MenuItem, Button} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {AppStore} from 'stores/index.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyDetailFetchIfNeeded, partyListInvalidate} from 'actions/party/party.jsx'
import {partyAdd, partyCreate, insertRelation, partyDelete} from 'actions/party/party.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {Utils} from 'components/index.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

import './PartyPage.less';

const keyModifier = Utils.getKeyModifier();

const keymap = {
    Party: {
        addParty: keyModifier + 'n',
        addRelation: keyModifier + 't',
        area1: keyModifier + '1',
        area2: keyModifier + '2',
        area3: keyModifier + '3',
    },
};
const shortcutManager = new ShortcutsManager(keymap);

/**
 * PARTY PAGE
 * *********************************************
 * Stránka osob
 */
class PartyPage extends AbstractReactComponent {


    static PropTypes = {
        splitter: React.PropTypes.object.isRequired,
        partyRegion: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
        refTables: React.PropTypes.object.isRequired
    };


    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(refPartyTypesFetchIfNeeded());         // načtení osob pro autory osoby
    }

    handleShortcuts = (action) => {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'addParty':
                this.refs.addParty.setOpen(true);
                break;
            case 'addRelation':
                this.refs.addRelation.setOpen(true);
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

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

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
        this.dispatch(partyAdd(partyTypeId, null, this.addParty, false));
    };

    /**
     * ADD RELATION
     * *********************************************
     * Uložení nového vztahu
     * @param data - data vztahu z formuláře
     */ 
    addRelation = (data) => {
        var entities = [];                                                          // seznam entit vztahu
        console.log(data.entities);
        for(var i = 0; i<data.entities.length; i++){                                // projdeme data entit z formuláře
            entities[entities.length] = {                                           // a přidáme je do seznamu nových entit
                source: data.entities[i].sources,                                   // poznámka ke vztahu o zdrojích dat
                record: {
                    "@class": ".RegRecordVO",
                    id: data.entities[i].record.id
                },
                roleType: {roleTypeId: data.entities[i].roleTypeId}                 // typ vztahu osoby a rejstříkové položky
            }
        }  
        var relation = {
            partyId: this.props.partyRegion.selectedPartyData.partyId,              // identifikátor osoby, které patří vkládaný vztah
            dateNote: data.dateNote,                                                // poznámka k dataci
            note: data.note,                                                        // poznámka
            from: data.from,                                                        // datace od
            to: data.to,                                                            // datace do
            relationEntities: entities,                                             // entity ve vztahu
            source: data.source,
            complementType:{relationTypeId:data.relationTypeId}                     // typ vztahu
        };   
        if(relation.from.textDate == "" || relation.from.textDate == null || relation.from.textDate == undefined){  
            relation.from = null;                                                   // pokud není zadaný textová část data, celý datum se ruší
        }
        if(relation.to.textDate == "" || relation.to.textDate == null || relation.to.textDate == undefined){  
            relation.to = null;                                                   // pokud není zadaný textová část data, celý datum se ruší
        }
        console.log(relation);
        this.dispatch(insertRelation(relation, this.props.partyRegion.selectedPartyID));  //uložení vztahu a znovunačtení osoby             
    };

    /**
     * HANDLE ADD RELATION
     * *********************************************
     * Kliknutí na volnu přidání nového vztahu
     */     
    handleAddRelation = (classType) => {
        var data = {                    
            partyTypeId: this.props.partyRegion.selectedPartyData.partyType.partyTypeId,
            partyId: this.props.partyRegion.selectedPartyID,
            note: "",
            dateNote:"",
            source: "",
            classType: classType,
            from: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId            
            },
            to: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId            
            },
            entities: [{
                record: null,
                roleTypeId : null,
                sources: '',
            }]
        };
        this.dispatch(modalDialogShow(this, this.props.partyRegion.selectedPartyData.record.record , <RelationForm initData={data} refTables={this.props.refTables} onSave={this.addRelation} />));
    };


    handleImport = () => {
        this.dispatch(
            modalDialogShow(this,
                i18n('import.title.party'),
                <ImportForm party/>
            )
        );
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
        const {userDetail, partyDetail, refTables} = this.props;

        const isSelected = partyDetail.id !== null;
        const altActions = [];
        if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL)) {
            altActions.push(
                <ControllableDropdownButton key='add-party' ref='add-party' id='add-party' title={<span className="dropContent"><Icon glyph='fa-download fa-fw' /><div><span className="btnText">{i18n('party.addParty')}</span></div></span>}>
                    {refTables.partyTypes.items.map(type => <MenuItem key={type.id} eventKey={type.id} onClick={this.handleAddParty.bind(this, type.id)}>{type.name}</MenuItem>)}
                </ControllableDropdownButton>
            );
            altActions.push(<Button key='export-party' onClick={this.handleImport}>
                <Icon glyph='fa-download fa-fw' />
                <div><span className="btnText">{i18n('ribbon.action.party.import')}</span></div>
            </Button>);
        }

        const itemActions = [];
        if (isSelected && partyDetail.fetched && !partyDetail.isFetching) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: partyDetail.data.record.scopeId})) {
                itemActions.push(
                    <ControllableDropdownButton key='add-relation' ref='addRelation' id='addRelation'
                                                title={<span className="dropContent"><Icon glyph='fa-download' /><div><span className="btnText">{i18n('party.relation.add')}</span></div></span>}>
                        {["B", "E", "R"].map(i => {
                            const name = i18n('party.relation.classType.' + i).toUpperCase();
                            return <MenuItem key={"classType"+i}
                                             onClick={this.handleAddRelation.bind(this,i)}>{name}</MenuItem>
                        })}
                    </ControllableDropdownButton>

                    //<Button onClick={this.handleAddRelation}><Icon glyph="fa-link" /><div><span className="btnText">{i18n('party.relation.add')}</span></div></Button>
                );
                itemActions.push(
                    <Button key='delete-relation' onClick={this.handleDeleteParty}><Icon glyph="fa-trash"/>
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

        return <Ribbon party altSection={altSection} itemSection={itemSection} {...this.props} />;
    };

    /**
     * RENDER
     * *********************************************
     * Vykreslení stránky pro osoby
     */ 
    render() {
        const {splitter, userDetail, partyDetail, refTables} = this.props;
        const canEdit = partyDetail.fetched &&
            !partyDetail.isFetching &&
            partyDetail.data &&
            userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: partyDetail.data.record.scopeId});

        const leftPanel = <PartyList />;

        const centerPanel = <PartyDetail
            refTables={refTables}
        />;

        return <Shortcuts name='Party' handler={this.handleShortcuts}>
            <PageLayout
                splitter={splitter}
                className='party-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        </Shortcuts>;
    }
}


function mapStateToProps(state) {
    const {app:{partyList, partyDetail}, splitter, refTables, userDetail} = state;
    return {
        partyList,
        partyDetail,
        splitter,
        refTables,
        userDetail,
    }
}

export default connect(mapStateToProps)(PartyPage);

