
import React from 'react';
import ReactDOM from 'react-dom';

import {Icon, i18n, AbstractReactComponent, NoFocusButton} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

var DescItemPartyRef = class DescItemPartyRef extends AbstractReactComponent {
    constructor(props) {
        super(props);
        //this.bindMethods('packetName', 'findType');
    }

    render() {
        const {descItem, locked} = this.props;

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <div className='desc-item-type-actions'><NoFocusButton title={i18n('subNodeForm.addDescItem')}><Icon glyph="fa-plus" /></NoFocusButton></div>
            </div>
        )
    }
}

module.exports = connect()(DescItemPartyRef);
