import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, RegistryField} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import './DescItemRecordRef.less'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import RegistrySelectPage from 'pages/select/RegistrySelectPage.jsx'
import {registryDetailFetchIfNeeded, registryListFilter, AREA_REGISTRY_LIST} from 'actions/registry/registry.jsx'
import classNames from 'classnames'
import {storeFromArea, objectById} from 'shared/utils'
import {MODAL_DIALOG_VARIANT} from 'constants'


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
        itemName: React.PropTypes.string,
        specName: React.PropTypes.string
    };

    handleSelectModule = ({onSelect, filterText, value}) => {
        const {hasSpecification, descItem, registryList:{filter}, fundName, nodeName, itemName, specName} = this.props;
        const open = (hasParty = false) => {
            this.dispatch(registryListFilter({...filter, text: filterText}));
            this.dispatch(registryDetailFetchIfNeeded(value ? value.id : null));
            this.dispatch(modalDialogShow(this, null, <RegistrySelectPage
                titles={[fundName, nodeName, itemName + (hasSpecification ? ': ' + specName : '')]}
                hasParty={hasParty} onSelect={(data) => {
                    onSelect(data);
                    this.dispatch(modalDialogHide());
            }}
            />, classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)));
        };

        if (hasSpecification) {
            WebApi.specificationHasParty(descItem.descItemSpecId).then(open);
        } else {
            open();
        }
    };

    render() {
        const {descItem, locked, singleDescItemTypeEdit, hasSpecification, readMode, cal, onDetail, typePrefix, ...otherProps} = this.props;
        const value = descItem.record ? descItem.record : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel onClick={onDetail.bind(this, descItem.record.recordId)} value={value.record} />
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
                    detail={typePrefix == "output" ? false : !disabled}
                    onSelectModule={this.handleSelectModule}
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, disabled, ['autocomplete-record'])}
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
        const {nodes} = fund;
        fundName = fund.name;
        const node = nodes.nodes[nodes.activeIndex];
        const {selectedSubNodeId} = node;
        const subNode = objectById(node.allChildNodes, selectedSubNodeId);
        subNode && subNode.name && (nodeName = subNode.name);
    }

    const registryList = storeFromArea(state, AREA_REGISTRY_LIST);
    return {
        registryList,
        fundName,
        nodeName
    }
}, null, null, { withRef: true })(DescItemRecordRef);
