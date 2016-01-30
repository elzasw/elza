/**
 * Stránka archivních pomůcek.
 */

require ('./PartyPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap'; 
import {Link, IndexLink} from 'react-router';
import {Icon, AbstractReactComponent, Ribbon, RibbonGroup, PartySearch, PartyDetail, PartyEntities, i18n} from 'components';
import {RelationForm, AddPartyPersonForm, AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm} from 'components';
import {ButtonGroup, MenuItem, DropdownButton, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {partyDetailFetchIfNeeded} from 'actions/party/party'
import {insertParty, insertRelation, deleteParty} from 'actions/party/party'


var PartyPage = class PartyPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.bindMethods(
            'buildRibbon', 
            'handleAddParty', 
            'handleCallAddParty', 
            'handleDeleteParty', 
            'handleCallDeleteParty', 
            'handleAddRelation', 
            'handleCallAddRelation'
        );
    }

    handleCallAddParty(data) {
        var partyType = '';                                     // typ osoby - je potreba uvest i jako specialni klivcove slovo 
        switch(data.partyTypeId){
            case 1: partyType = '.ParPersonVO'; break;          // typ osoby osoba
            case 2: partyType = '.ParDynastyVO'; break;         // typ osoby rod
            case 3: partyType = '.ParPartyGroupVO'; break;      // typ osoby korporace
            case 4: partyType = '.ParEventVO'; break;           // typ osoby docasna korporace - udalost
        }
        var party = {
            '@type': partyType, 
            partyType: {
                partyTypeId: data.partyTypeId
            },
            genealogy: data.nameMain,
            scope: '',
            record: {
                registerTypeId: data.recordTypeId,              // typ záznamu 
                scopeId:1                                       //trida rejstriku 
            },
            from: {
                textDate: data.validFrom,
                calendarTypeId: data.calendarTypeIdFrom
            },
            to: {
                textDate: data.validTo,
                calendarTypeId: data.calendarTypeIdTo
            },
            partyNames : [{
                nameFormType: {
                    nameFormTypeId: data.nameFormTypeId
                },
                displayName: data.nameMain,
                mainPart: data.nameMain,
                otherPart: data.nameOther,
                degreeBefore: data.degreeBefore,
                degreeAfter: data.degreeAfter,
                prefferedName: true,
                validFrom: {
                    textDate: data.validFrom,
                    calendarTypeId: data.calendarTypeIdFrom
                },
                validTo: {
                    textDate: data.validTo,
                    calendarTypeId: data.calendarTypeIdTo
                }
            }]
        }
        this.dispatch(insertParty(party)); 
    }

    handleAddParty(partyTypeId, event) {
        var data = {
            partyTypeId: partyTypeId
        }
        switch(partyTypeId){
            case 1:
                // zobrazení formuláře fyzicke osoby
                this.dispatch(modalDialogShow(this, i18n('party.addParty') , <AddPartyPersonForm initData={data} onSubmit={this.handleCallAddParty} />));
                break; 
            case 2:
                // zobrazení formuláře rodu
                this.dispatch(modalDialogShow(this, i18n('party.addPartyDynasty') , <AddPartyDynastyForm initData={data} onSubmit={this.handleCallAddParty} />));
                break;
            case 3:
                // zobrazení formuláře korporace
                this.dispatch(modalDialogShow(this, i18n('party.addPartyGroup') , <AddPartyGroupForm initData={data} onSubmit={this.handleCallAddParty} />));
                break;
            case 4:
                // zobrazení formuláře dočasné korporace
                this.dispatch(modalDialogShow(this, i18n('party.addPartyEvent') , <AddPartyEventForm initData={data} onSubmit={this.handleCallAddParty} />));
                break; 
            default:
                // zobrazení formuláře jine osoby - ostatni
                this.dispatch(modalDialogShow(this, i18n('party.addPartyOther') , <AddPartyOtherForm initData={data} onSubmit={this.handleCallAddParty} />));
                break; 
        }
    }

    handleCallAddRelation(data) {
        var entities = [{
            source: "aaa",
            record: {recordId: 1},
            roleType: {roleTypeId: 1}
        },{
            source: "aaa",
            record: {recordId: 2},
            roleType: {roleTypeId: 1}       
        }];
        var relation = {
            partyId: this.props.partyRegion.selectedPartyID,
            dateNote: data.dateNote,
            note: data.note,
            from: {
                    textDate: "aa",
                    calendarTypeId: data.calendarTypeIdFrom
            },
            to: {
                    textDate: "bb",
                    calendarTypeId: data.calendarTypeIdTo
            },
            relationEntities: entities,
            complementType:{relationTypeId:1}
        };   
        this.dispatch(insertRelation(relation, this.props.partyRegion.selectedPartyID));               
    }

    handleAddRelation(){
        var data = {
            partyTypeId: this.props.partyRegion.selectedPartyData.partyType.partyTypeId,
            partyId: this.props.partyRegion.selectedPartyID,
            entities: [{
                recordId: null,
                roleTypeId : null,
                sources: 'aaa',
            }]
        }
        this.dispatch(modalDialogShow(this, this.props.partyRegion.selectedPartyData.record.record , <RelationForm initData={data} refTables={this.props.refTables} onSubmit={this.handleCallAddRelation} />));
    }

    handleDeleteParty(){
        var result = confirm(i18n('party.delete.confirm'));
        if (result) {
            this.dispatch(this.handleCallDeleteParty());
        }
    }
    handleCallDeleteParty() {
        this.dispatch(deleteParty(this.props.partyRegion.selectedPartyID, this.props.partyRegion.filterText));
    }    

    buildRibbon() {
        var isSelected = this.props.partyRegion.selectedPartyID ? true : false;
        var altActions = [];
        altActions.push(
            <DropdownButton title={<span className="dropContent"><Icon glyph='fa-download' /><div><span className="btnText">{i18n('party.addParty')}</span></div></span>}>
                {this.props.refTables.partyTypes.items.map(i=> {return <MenuItem eventKey="{i.partyTypeId}" onClick={this.handleAddParty.bind(this, i.partyTypeId)}>{i.name}</MenuItem>})}
            </DropdownButton>
        );

        altActions.push(
            <Button><Icon glyph='fa-download' /><div><span className="btnText">{i18n('ribbon.action.party.import')}</span></div></Button>
        );
        var itemActions = [];
        if (isSelected) {
            itemActions.push(
                <Button onClick={this.handleAddRelation}><Icon glyph="fa-link" /><div><span className="btnText">{i18n('party.relation.add')}</span></div></Button>
            );
            itemActions.push(
                <Button onClick={this.handleDeleteParty}><Icon glyph="fa-trash" /><div><span className="btnText">{i18n('party.delete.button')}</span></div></Button>
            );
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }
        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon party altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    render() {
        var leftPanel = (
            <PartySearch 
                items={this.props.partyRegion.items} 
                selectedPartyID={this.props.partyRegion.selectedPartyID}
                filterText={this.props.partyRegion.filterText} 
            />
        )
        
        var centerPanel = (
            <PartyDetail 
                refTables={this.props.refTables} 
                partyRegion={this.props.partyRegion} 
            />
        )

        var rightPanel = (
            <PartyEntities 
                partyRegion={this.props.partyRegion}
            />
        )

        return (
            <PageLayout
                className='party-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}


function mapStateToProps(state) {
    const {partyRegion, refTables} = state
    return {
        partyRegion,
        refTables
    }
}

module.exports = connect(mapStateToProps)(PartyPage);

