/**
 * Stránka archivních pomůcek.
 */

require ('./PartyPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {AbstractReactComponent, Ribbon, RibbonGroup, PartySearch, PartyDetail, AddPartyForm, i18n} from 'components';
import {ButtonGroup, MenuItem, DropdownButton, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {partyDetailFetchIfNeeded} from 'actions/party/party'
import {insertParty} from 'actions/party/party'

var PartyPage = class PartyPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.bindMethods('buildRibbon', 'handleAddParty', 'handleCallAddParty', 'addParty');
    }

    handleCallAddParty(data) {
        this.dispatch(insertParty('ParPersonEditVO', data.nameFormTypeId, data.nameMain, data.nameOther, data.degreeBefore, data.degreeAfter, data.validFrom, data.validTo));
    }

    handleAddParty() {
        this.dispatch(modalDialogShow(this, i18n('party.addParty') , <AddPartyForm create onSubmit={this.handleCallAddParty} />));
    }

    addParty(data){
        var id = WebApi.insertParty('ParPersonEditVO', data.partyTypeId, data.nameMain, data.nameOther, data.degreeBefore, data.degreeAfter, data.validFrom, data.validTo)
            .then(this.dispatch(modalDialogHide()));
        console.log(id);    
        this.dispatch(partyDetailFetchIfNeeded(3));
    }

    buildRibbon() {
        var isSelected = this.props.partyRegion.selectedPartyID ? true : false;
        var altActions = [];
        altActions.push(
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Nová osoba</span></div></span>}>
                {this.props.refTables.partyTypes.items.map(i=> {return <MenuItem eventKey="{i.id}" onClick={this.handleAddParty} value={i.id}>{i.name}</MenuItem>})}
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
                <Button><Glyphicon glyph="trash" /><div><span className="btnText">Smazat osobu</span></div></Button>
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
            <PartyDetail selectedPartyData={this.props.partyRegion.selectedPartyData} />
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

