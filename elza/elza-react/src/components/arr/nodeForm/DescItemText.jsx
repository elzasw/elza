/**
 * Input prvek pro desc item - typ STRING.
 */
import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {decorateValue, inputValue} from './DescItemUtils.jsx';
import {DescItemLabel} from './DescItemLabel';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import TextareaAutosize from 'react-textarea-autosize';
import './DescItemText.scss';
import {CLS_CALCULABLE} from "../../../constants";

class DescItemText extends AbstractReactComponent {
    textarea = null;
    focus = () => {
        this.textarea && this.textarea.focus();
    };

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = descItem.value;

        if (readMode) {
            return <DescItemLabel
                value={value}
                hideTooltip={true}
                cal={cal}
                isValueUndefined={descItem.undefined}
                isValueInhibited={descItem.inhibited}
            />;
        }

        value = descItem.undefined ? i18n('subNodeForm.descItemType.undefinedValue') : inputValue(value);

        let cls = [];
        let textareaProps = {
            key: 'val',
            disabled: locked || descItem.undefined,
        };

        if (cal) {
            cls.push(CLS_CALCULABLE);

            textareaProps.key = 'calc';
            textareaProps.disabled = true;
            textareaProps.onBlur = null;
            textareaProps.onFocus = null;
            textareaProps.placeholder = i18n('subNodeForm.descItemType.calculable');
            if (value) {
                textareaProps.value = value;
            }
        } else {
            textareaProps.onChange = e => !cal && this.props.onChange(e.target.value);
            textareaProps.value = value;
        }

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.text.format">
                    <TextareaAutosize
                        maxRows={12}
                        rows={3}
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        {...textareaProps}
                        innerRef={ref => (this.textarea = ref)}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemText;
