/**
 * Komponenta hledání osob
 */

require ('./partySearch.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search} from 'components';
import {AppActions} from 'stores';

var PartySearch = class PartySearch extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return <Search filterText={"AAA"}/>
    }
}

module.exports = connect()(PartySearch);
