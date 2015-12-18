/**
 * Komponenta detailu osoby
 */

require ('./PartyDetail.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search} from 'components';
import {AppActions} from 'stores';

import {findPartyFetchIfNeeded} from 'actions/party/party.jsx'

var PartyDetail = class PartySearch extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }
    
    render() {
        var data = this.props.selectedPartyData;
        if(!data){
            return <div>Nenalezeno</div>
        }
        return  <div>
                    <h1>Osoba {data.id}</h1>
                </div>
    }
}

module.exports = connect()(PartyDetail);
