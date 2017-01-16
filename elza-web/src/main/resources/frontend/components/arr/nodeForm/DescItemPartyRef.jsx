import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {i18n, AbstractReactComponent, PartyField} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

import './DescItemPartyRef.less'

/**
 * Obal pro PartyField
 */
class DescItemPartyRef extends AbstractReactComponent {

    static PropTypes = {
        onChange: React.PropTypes.func.isRequired,
        onCreateParty: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func.isRequired,
        versionId: React.PropTypes.number
    };

    focus = () => {
        this.refs.partyField.refs.wrappedInstance.focus();
    };

    render() {
        const {descItem, locked, singleDescItemTypeEdit, readMode, cal, onDetail, ...otherProps} = this.props;
        const value = descItem.party ? descItem.party : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel onClick={onDetail.bind(this, descItem.party.id)} value={value.record.record} />;
            } else {
                return <DescItemLabel value={cal ? i18n("subNodeForm.descItemType.calculable") : ""} cal={cal} />
            }
        }

        return <div className='desc-item-value desc-item-value-parts'>
            <ItemTooltipWrapper tooltipTitle="dataType.partyRef.format" className="tooltipWrapper">
                <PartyField
                    ref="partyField"
                    {...otherProps}
                    value={value}
                    detail={true}
                    footerButtons={false}
                    footer={!singleDescItemTypeEdit}
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-party'])}
                />
            </ItemTooltipWrapper>
        </div>
    }
}

export default connect(null, null, null, { withRef: true })(DescItemPartyRef);
