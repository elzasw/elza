import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, RegistryField} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import './DescItemRecordRef.less'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

class DescItemRecordRef extends AbstractReactComponent {

    focus() {
        this.refs.registryField.refs.wrappedInstance.focus()
    }

    static defaultProps = {
        hasSpecification: false,
    };

    static PropTypes = {
        descItem: React.PropTypes.object.isRequired,
        hasSpecification: React.PropTypes.bool,
    };

    render() {
        const {descItem, locked, singleDescItemTypeEdit, hasSpecification, readMode, cal, ...otherProps} = this.props;
        const value = descItem.record ? descItem.record : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel onClick={this.props.onDetail.bind(this, descItem.record.recordId)} value={value.record} />
            } else {
                return <DescItemLabel value={cal ? i18n("subNodeForm.descItemType.calculable") : ""} cal={cal} />
            }
        }


        let disabled = locked;
        if (hasSpecification && !descItem.descItemSpecId) {
            disabled = true;
        }

        return <div className='desc-item-value desc-item-value-parts'>
            <ItemTooltipWrapper tooltipTitle="dataType.recordRef.format" className="tooltipWrapper">
                <RegistryField
                    ref='registryField'
                    {...otherProps}
                    itemSpecId={descItem.descItemSpecId}
                    value={value}
                    footer={!singleDescItemTypeEdit}
                    footerButtons={false}
                    detail={!disabled}
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, disabled, ['autocomplete-record'])}
                />
            </ItemTooltipWrapper>
        </div>
    }
}


export default connect(null, null, null, { withRef: true })(DescItemRecordRef);
