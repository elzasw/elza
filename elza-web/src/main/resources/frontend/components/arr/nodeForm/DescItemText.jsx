/**
 * Input prvek pro desc item - typ STRING.
 */

require ('./DescItemText.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

var DescItemText = class DescItemText extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('focus')
    }

    focus() {
        this.refs.focusEl.focus()
    }

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : descItem.value;

        if (readMode) {
            return (
                <DescItemLabel value={value} cal={cal} />
            )
        }

        let cls = [];
        if (cal) {
            cls.push("calculable");
        }

        return (
            <div className='desc-item-value'>
                <ItemTooltipWrapper tooltipTitle="dataType.text.format">
                    <textarea
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref='focusEl'
                        type="text"
                        disabled={locked}
                        value={value}
                        onChange={(e) => this.props.onChange(e.target.value)}
                    />
                </ItemTooltipWrapper>
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemText);
