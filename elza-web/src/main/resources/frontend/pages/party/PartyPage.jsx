/**
 * Stránka archivních pomůcek.
 */

require ('./PartyPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {AbstractReactComponent, Ribbon, RibbonGroup, PartySearch, PartyDetail, DropTree} from 'components';
import {ButtonGroup, MenuItem, DropdownButton, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'

import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party.jsx'

var PartyPage = class PartyPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('buildRibbon');
    }

    buildRibbon() {
        var isSelected = this.props.partyRegion.selectedPartyID ? true : false;

        var actions = [];
        actions.push(
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Nová osoba</span></div></span>}>
                <MenuItem eventKey="1">Osoba</MenuItem>
                <MenuItem eventKey="2">Rod</MenuItem>
                <MenuItem eventKey="3">Korporace</MenuItem>
                <MenuItem eventKey="4">Dočasná korporace</MenuItem>
            </DropdownButton>
        );
        isSelected && actions.push(
            <Button><Glyphicon glyph="link" /><div><span className="btnText">Nový vztah</span></div></Button>
        );
        actions.push(
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Import</span></div></span>}>
                <MenuItem eventKey="1">Osob</MenuItem>
            </DropdownButton>
        );
        isSelected && actions.push(
            <Button><Glyphicon glyph="ok" /><div><span className="btnText">Validace</span></div></Button>
        );
        isSelected && actions.push(
            <Button><Glyphicon glyph="trash" /><div><span className="btnText">Smazat osobu</span></div></Button>
        );

        var altSection = <RibbonGroup className="large">{actions}</RibbonGroup>

        return (
            <Ribbon party altSection={altSection} {...this.props} />
        )
    }

    render() {

         var items =
            [
                {
                    id: 1, name: 'Stromy', childrens: [
                        {id: 17, name : 'Baobab'},
                        {id: 18, name : 'Dub'},
                        {id: 19, name : 'Javor'} 
                    ]
                },{
                    id: 2, name: 'Kytky', childrens: [
                        {id: 14, name : 'Pampeliška'},
                        {id: 15, name : 'Kopretina'},
                        {id: 16, name : 'Chrpa'}  
                    ]
                },{
                    id: 3, name: 'Zvířáta', childrens : [
                        {
                            id: 5, name : 'Hezký zvířata', childrens : [
                                {id: 8, name : 'Tygr'},
                                {id: 6, name : 'Medvěd'},
                                {id: 7, name : 'Orel'}
                            ]
                        },{
                            id: 9, name : 'Ošklivý zvířata', childrens : [
                                {id: 10, name : 'Šnek'},
                                {id: 11, name : 'Vosa'},
                                {id: 12, name : 'Hyena'},
                                {id: 13, name : 'Prase'}
                            ]
                        },    
                    ]
                },{
                    id: 4, name: 'Kameny', childrens: [
                        {id: 20, name : 'Opál'},
                        {id: 21, name : 'Achát'},
                        {id: 22, name : 'Živec'}
                    ]
                }
            ];

        var leftPanel = (
            <div>
                <PartySearch 
                    items={this.props.partyRegion.items} 
                    selectedPartyID={this.props.partyRegion.selectedPartyID}
                    filterText={this.props.partyRegion.filterText} 
                />
                <DropTree 
                    items = {items} 
                    selectedItemID = {20}
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

