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

import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party.jsx'

var PartyPage = class PartyPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('buildRibbon', 'handleAddParty', 'handleCallAddParty');
    }

    handleCallAddParty(data) {
        WebApi.insertParty('ParPersonEditVO', data.partyTypeId, data.nameMain, data.nameOther, data.degreeBefore, data.degreeAfter, data.validFrom, data.validTo).then(this.dispatch(modalDialogHide()));
        this.dispatch(modalDialogHide());
        this.dispatch(partyDetailFetchIfNeeded(3));
    }
    
    handleAddParty() {
        this.dispatch(modalDialogShow(this, i18n('party.addParty') , <AddPartyForm create onSubmit={this.handleCallAddParty} />));
    }

    buildRibbon() {
        var isSelected = this.props.partyRegion.selectedPartyID ? true : false;

        var altActions = [];
        altActions.push(
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Nová osoba</span></div></span>}>
                <MenuItem onClick={this.handleAddParty} eventKey="1">Osoba</MenuItem>
                <MenuItem eventKey="2">Rod</MenuItem>
                <MenuItem eventKey="3">Korporace</MenuItem>
                <MenuItem eventKey="4">Dočasná korporace</MenuItem>
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
    const {partyRegion} = state
    return {
        partyRegion
    }
}

module.exports = connect(mapStateToProps)(PartyPage);

