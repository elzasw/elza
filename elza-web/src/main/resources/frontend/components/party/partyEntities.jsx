/**
 * Entity pro vybranou osobu
 */

require ('./partyEntities.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search, i18n} from 'components';
import {AppActions} from 'stores';


var PartyEntities = class PartyEntities extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }
    
    render() {
        return  <div>
                    aaa2
                </div>
    }
}

module.exports = connect()(PartyEntities);
