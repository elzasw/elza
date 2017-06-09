import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {i18n, AbstractReactComponent, PartyField} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import {storeFromArea, objectById} from 'shared/utils'
import {partyDetailFetchIfNeeded, partyListFilter, partyDetailClear, AREA_PARTY_LIST} from 'actions/party/party.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import classNames from 'classnames'
import {MODAL_DIALOG_VARIANT} from 'constants'
import {PartySelectPage} from 'pages'


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


    handleSelectModule = ({onSelect, filterText, value}) => {
        const {partyList:{filter},  fundName, nodeName, itemName, specName, hasSpecification} = this.props;
        this.dispatch(partyListFilter({...filter, text:filterText}));
        this.dispatch(partyDetailFetchIfNeeded(value ? value.id : null));
        this.dispatch(modalDialogShow(this, null, <PartySelectPage
            titles={[fundName, nodeName, itemName + (hasSpecification ? ': ' + specName : '')]}
            onSelect={(data) => {
                onSelect(data);
                this.dispatch(partyListFilter({text:null, type:null, itemSpecId: null}));
                this.dispatch(partyDetailClear());
                this.dispatch(modalDialogHide());
            }}
        />, classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)));
    };

    render() {
        const {descItem, locked, singleDescItemTypeEdit, readMode, cal, onDetail, typePrefix, ...otherProps} = this.props;
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
                    detail={typePrefix != "output" || !cal}
                    footerButtons={false}
                    footer={!singleDescItemTypeEdit}
                    onSelectModule={this.handleSelectModule}
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-party'])}
                />
            </ItemTooltipWrapper>
        </div>
    }
}

export default connect((state, props) => {
    let fundName = null, nodeName = null;
    if (props.typePrefix != "output") {
        const {arrRegion:{activeIndex,funds}} = state;
        const fund = funds[activeIndex];
        fundName = fund.name;
        const {nodes} = fund;
        const node = nodes.nodes[nodes.activeIndex];
        const {selectedSubNodeId} = node;
        const subNode = objectById(node.childNodes, selectedSubNodeId);
        subNode && subNode.name && (nodeName = subNode.name);
    }

    const partyList = storeFromArea(state, AREA_PARTY_LIST);
    return {
        partyList,
        fundName,
        nodeName
    }
}, null, null, { withRef: true })(DescItemPartyRef);
