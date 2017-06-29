/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components/shared';
import {connect} from 'react-redux'
import {normalizeDouble} from 'components/validate.jsx'
import {decorateValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

var classNames = require('classnames');

var DescItemDecimal = class DescItemDecimal extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChange', 'focus');

        this.state = {value: this.convertToClient(props.descItem.value)}
    }

    componentWillReceiveProps(nextProps) {
        this.setState({value: this.convertToClient(nextProps.descItem.value)})
    }

    convertToClient(value) {
        if (typeof value !== 'undefined' && value !== '' && value !== null) {
            return ('' + value).replace(/(\.)/, ',')
        } else {
            return ''
        }
    }

    convertFromClient(value) {
        if (typeof value !== 'undefined' && value !== '' && value !== null) {
            return value.replace(/(,)/, '.')
        } else {
            return ''
        }
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleChange(e) {
        var newValue = normalizeDouble(e.target.value);

        var value = this.convertFromClient(newValue)

        var prevValue
        if (typeof this.props.descItem.value !== 'undefined' && this.props.descItem.value !== '' && this.props.descItem.value !== null) {
            prevValue = '' + this.props.descItem.value
        } else {
            prevValue = ''
        }

        if (value != prevValue) {
            this.props.onChange(value);
        }
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
                <ItemTooltipWrapper tooltipTitle="dataType.decimal.format">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref='focusEl'
                        type="text"
                        disabled={locked}
                        onChange={this.handleChange}
                        value={cal && this.state.value == '' ? value : this.state.value}
                    />
                </ItemTooltipWrapper>
            </div>
        )
    }
}

export default connect(null, null, null, { withRef: true })(DescItemDecimal);
