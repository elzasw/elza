/**
 * Obal pro PartyField
 */
import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {i18n, AbstractReactComponent} from 'components/shared';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import {storeFromArea, objectById} from 'shared/utils'
import {partyDetailFetchIfNeeded, partyListFilter, partyDetailClear, AREA_PARTY_LIST} from 'actions/party/party.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import classNames from 'classnames'
import {MODAL_DIALOG_VARIANT} from '../../../constants.tsx'


import './DescItemPartyRef.scss'
import PartyField from "../../party/PartyField";
import PartySelectPage from "../../../pages/select/PartySelectPage";

class DescItemPartyRef extends AbstractReactComponent {

    static propTypes = {
        onChange: PropTypes.func.isRequired,
        onCreateParty: PropTypes.func.isRequired,
        onDetail: PropTypes.func.isRequired,
        versionId: PropTypes.number
    };

    focus = () => {
        this.refs.partyField.wrappedInstance.focus();
    };


    handleSelectModule = ({onSelect, filterText, value}) => {
        const {partyList:{filter},  fundName, nodeName, itemName, specName, hasSpecification} = this.props;
        this.props.dispatch(partyListFilter({...filter, text:filterText}));
        this.props.dispatch(partyDetailFetchIfNeeded(value ? value.id : null));
        this.props.dispatch(modalDialogShow(this, null, <PartySelectPage
            titles={[fundName, nodeName, itemName + (hasSpecification ? ': ' + specName : '')]}
            onSelect={(data) => {
                onSelect(data);
                this.props.dispatch(partyListFilter({text:null, type:null, itemSpecId: null}));
                this.props.dispatch(partyDetailClear());
                this.props.dispatch(modalDialogHide());
            }}
        />, classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)));
    };

    render() {
        const {descItem, locked, singleDescItemTypeEdit, readMode, cal, onDetail, typePrefix, ...otherProps} = this.props;
        const value = descItem.party ? descItem.party : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel onClick={onDetail.bind(this, descItem.party.id)} notIdentified={descItem.undefined} value={value.accessPoint.record} />;
            } else {
                return <DescItemLabel value={cal ? i18n("subNodeForm.descItemType.calculable") : ""} cal={cal} notIdentified={descItem.undefined} />
            }
        }

        return <div className='desc-item-value desc-item-value-parts'>
            <ItemTooltipWrapper tooltipTitle="dataType.partyRef.format" className="tooltipWrapper">
                <PartyField
                    ref="partyField"
                    {...otherProps}
                    value={value}
                    detail={!descItem.undefined && (typePrefix != "output" || !cal)}
                    footerButtons={false}
                    footer={!singleDescItemTypeEdit}
                    undefined={descItem.undefined}
                    onSelectModule={this.handleSelectModule}
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked || descItem.undefined, ['autocomplete-party'])}
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
})(DescItemPartyRef);
