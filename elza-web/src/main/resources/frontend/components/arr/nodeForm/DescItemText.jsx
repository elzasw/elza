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
        var textareaProps = {
            key: "val",
            disabled: locked,
            defaultValue: ""
        }

        if(cal){
            cls.push("calculable");

            textareaProps.key = "calc";
            textareaProps.disabled = true;
            textareaProps.onBlur = null;
            textareaProps.onFocus = null;
            textareaProps.defaultValue = i18n("subNodeForm.descItemType.calculable");
            if(value){
                textareaProps.value = value;
            }
        } else {
            textareaProps.ref = "focusEl";
            textareaProps.onChange = (e) => !cal && this.props.onChange(e.target.value);
            textareaProps.value = value;
        }



        return (
            <div className='desc-item-value'>
                <ItemTooltipWrapper tooltipTitle="dataType.text.format">
                    <textarea
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        {...textareaProps}
                    />
                </ItemTooltipWrapper>
            </div>
        )
    }
}

export default connect(null, null, null, { withRef: true })(DescItemText);
