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
        let value = descItem.value;

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
                    {cal ? <textarea
                            {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                            key="calc"
                            defaultValue={i18n("subNodeForm.descItemType.calculable")}
                            disabled={true}
                            onBlur={null}
                            onFocus={null}
                        /> :
                    <textarea
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        key="val"
                        ref='focusEl'
                        disabled={locked}
                        defaultValue={''}
                        value={value}
                        onChange={(e) => !cal && this.props.onChange(e.target.value)}
                    />}
                </ItemTooltipWrapper>
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemText);
