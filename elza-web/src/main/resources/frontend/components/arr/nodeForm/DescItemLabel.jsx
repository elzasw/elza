require ('./DescItemLabel.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, Icon} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {Button} from 'react-bootstrap';

var DescItemLabel = class DescItemLabel extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {value, onClick} = this.props;

        let renderItem;

        if (onClick == null) {
            renderItem = value;
        } else {
            renderItem = <a href="#" onClick={onClick}>{value}</a>;
        }

        return (
            <div title={value} className='desc-item-label-value'>
                {renderItem}
            </div>
        )
    }
}



module.exports = connect(null, null, null, { withRef: true })(DescItemLabel);
