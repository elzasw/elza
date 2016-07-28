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

        // Sestavení hodnoty - změna znaku < na entitu, nahrazení enterů <br/>
        var updatedValue = value ? ("" + value).replace(/\</g,"&lt;").replace(/(?:\r\n|\r|\n)/g, '<br />') : "";

        let renderItem;
        if (onClick == null) {
            renderItem = <div dangerouslySetInnerHTML={{__html: updatedValue}}></div>;
        } else {
            renderItem = <a href="#" onClick={onClick} dangerouslySetInnerHTML={{__html: updatedValue}}></a>;
        }

        return (
            <div title={value} className='desc-item-label-value'>
                {renderItem}
            </div>
        )
    }
}



module.exports = connect(null, null, null, { withRef: true })(DescItemLabel);
