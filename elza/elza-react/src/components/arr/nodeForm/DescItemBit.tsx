import React from 'react';
import {AbstractReactComponent, i18n} from '../../../components/shared';
import {normalizeString} from 'components/validate';
import {decorateValue, inputValue} from './DescItemUtils';
import DescItemLabel from './DescItemLabel';
import ItemTooltipWrapper from './ItemTooltipWrapper';
import {CLS_CALCULABLE} from '../../../constants';
import {objectFromWKT} from '../../Utils';
import {FormControl} from 'react-bootstrap';
import {DescItemComponentProps} from './DescItemTypes';

type Props = DescItemComponentProps<boolean | null> & {
    required: boolean;
};

class DescItemBit extends AbstractReactComponent<Props> {
    private readonly focusEl: React.RefObject<HTMLInputElement>;

    constructor(props) {
        super(props);
        this.focusEl = React.createRef();
    }

    focus = () => {
        this.focusEl.current?.focus();
    };

    handleChange = e => {
        const newValue = e.target.value === '' ? null : Boolean(parseInt(e.target.value));

        if (newValue !== this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    };

    render() {
        const {descItem, locked, readMode, cal, required} = this.props;
        let value =
            cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : inputValue(descItem.value);

        if (readMode) {
            return (
                <DescItemLabel
                    value={i18n(Boolean(descItem.value) ? 'global.title.yes' : 'global.title.no')}
                    cal={cal}
                    notIdentified={descItem.undefined}
                />
            );
        }

        let cls: string[] = [];
        let inputEl;
        if (cal) {
            cls.push(CLS_CALCULABLE);
            inputEl = (
                <FormControl
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                    type={'text'}
                    innerRef={this.focusEl}
                    disabled={locked || descItem.undefined}
                    value={value}
                />
            );
        } else {
            if (value === true) {
                value = 1;
            } else if (value === false) {
                value = 0;
            }
            inputEl = (
                <FormControl
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                    as={'select'}
                    innerRef={this.focusEl}
                    disabled={locked || descItem.undefined}
                    value={value}
                    placeholder={'Vyberte'}
                    onChange={this.handleChange}
                >
                    {(descItem.value === null || !required) && <option value={''}></option>}
                    <option value={1}>{i18n('global.title.yes')}</option>
                    <option value={0}>{i18n('global.title.no')}</option>
                </FormControl>
            );
        }

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.string.format">{inputEl}</ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemBit;
