/**
 * Stránka archivních pomůcek.
 */

require ('./PartyPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap'; 
import {Link, IndexLink} from 'react-router';
import {AbstractReactComponent, Ribbon, RibbonGroup, PartySearch, PartyDetail, i18n} from 'components';
import {AddPartyPersonForm, AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm} from 'components';
import {ButtonGroup, MenuItem, DropdownButton, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {partyDetailFetchIfNeeded} from 'actions/party/party'
import {insertParty, deleteParty} from 'actions/party/party'


var PartyPage = class PartyPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.bindMethods('buildRibbon', 'handleAddParty', 'handleCallAddParty', 'handleDeleteParty', 'handleCallDeleteParty');
    }

    handleCallAddParty(data) {
        switch(data.partyTypeId){
            case 1:
                // vytvoření fyzicke osoby
                this.dispatch(insertParty('ParPersonEditVO', this.props.partyRegion.filterText, data.partyTypeId, data.nameFormTypeId, data.nameMain, data.nameOther, data.validRange, data.calendarType, data.degreeBefore, data.degreeAfter));
                break; 
            case 2:
                // vytvoření rodu
                this.dispatch(insertParty('ParDynastyEditVO', this.props.partyRegion.filterText, data.partyTypeId, data.nameFormTypeId, data.nameMain, data.nameOther, data.validRange, data.calendarType));
                break;
            case 3:
                // vytvoření korporace
                this.dispatch(insertParty('ParPartyGroupEditVO', this.props.partyRegion.filterText, data.partyTypeId, data.nameFormTypeId, data.nameMain, data.nameOther, data.validRange, data.calendarType, '', '', ''));
                break;
            case 4:
                // vytvoření dočasné korporace
                this.dispatch(insertParty('ParEventEditVO', this.props.partyRegion.filterText, data.partyTypeId, data.nameFormTypeId, data.nameMain, data.nameOther, data.validRange, data.calendarType));
                break; 
            default:
                // vytvoření jine osoby - ostatni
                this.dispatch(insertParty('', this.props.partyRegion.filterText, data.partyTypeId, data.nameFormTypeId, data.nameMain, data.nameOther, data.validRange, data.calendarType));
                break; 
        }
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
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Nová osoba</span></div></span>}>
                {this.props.refTables.partyTypes.items.map(i=> {return <MenuItem eventKey="{i.partyTypeId}" onClick={this.handleAddParty.bind(this, i.partyTypeId)}>{i.name}</MenuItem>})}
            </DropdownButton>
        );
        altActions.push(
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Import</span></div></span>}>
                <MenuItem eventKey="1">Osob</MenuItem>
            </DropdownButton>
        );
        var itemActions = [];
        if (isSelected) {
            itemActions.push(
                <Button><Glyphicon glyph="link" /><div><span className="btnText">Nový vztah</span></div></Button>
            );
            itemActions.push(
                <Button><Glyphicon glyph="ok" /><div><span className="btnText">Validace</span></div></Button>
            );
            itemActions.push(
                <Button onClick={this.handleDeleteParty}><Glyphicon glyph="trash" /><div><span className="btnText">ASmazat osobu</span></div></Button>
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
            <div>
                <PartySearch 
                    items={this.props.partyRegion.items} 
                    selectedPartyID={this.props.partyRegion.selectedPartyID}
                    filterText={this.props.partyRegion.filterText} 
                />
            </div>
        )
        
        var centerPanel = (
            <PartyDetail refTables={this.props.refTables} selectedPartyData={this.props.partyRegion.selectedPartyData} />
        )

        var rightPanel = (
            <div>
                
            </div>
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

