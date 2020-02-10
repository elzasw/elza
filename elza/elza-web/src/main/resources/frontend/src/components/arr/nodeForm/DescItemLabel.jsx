require ('./DescItemLabel.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, Icon, i18n} from 'components/shared';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {Button} from 'react-bootstrap';
var classNames = require('classnames');

var DescItemLabel = class DescItemLabel extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {value, onClick, cal, notIdentified} = this.props;

        let cls = ['desc-item-label-value'];
        if (cal) {
            cls.push("calculable");
        }

        // Sestavení hodnoty - změna znaku < na entitu, nahrazení enterů <br/>
        var updatedValue = value ? ("" + value).replace(/\</g,"&lt;").replace(/(?:\r\n|\r|\n)/g, '<br />') : "";

        let renderItem;
        if (onClick == null) {
            renderItem = <div dangerouslySetInnerHTML={{__html: updatedValue}}></div>;
        } else {
            renderItem = <a style={{'cursor': 'pointer'}} onClick={onClick} dangerouslySetInnerHTML={{__html: updatedValue}}></a>;
        }

        if (notIdentified) {
            renderItem = <i>{i18n("subNodeForm.descItemType.notIdentified")}</i>
        }

        return (
            <div title={value} className={classNames(cls)}>
                {renderItem}
            </div>
        )
    }
}



export default connect(null, null, null, { withRef: true })(DescItemLabel);
