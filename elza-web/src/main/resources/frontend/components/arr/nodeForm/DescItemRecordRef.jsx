import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent} from 'components/shared';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import './DescItemRecordRef.less'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import RegistrySelectPage from 'pages/select/RegistrySelectPage.jsx'
import {registryDetailFetchIfNeeded, registryListFilter, registryDetailClear, AREA_REGISTRY_LIST} from 'actions/registry/registry.jsx'
import {partyDetailFetchIfNeeded, partyListFilter, partyDetailClear, AREA_PARTY_LIST} from 'actions/party/party.jsx'
import classNames from 'classnames'
import {storeFromArea, objectById} from 'shared/utils'
import {MODAL_DIALOG_VARIANT} from 'constants.jsx'
import RegistryField from "../../registry/RegistryField";


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
        const {hasSpecification, descItem, registryList, partyList, fundName, nodeName, itemName, specName} = this.props;
        console.log(filterText, "aaa");
        const open = (hasParty = false) => {
            if (hasParty) {
                this.dispatch(partyListFilter({...partyList.filter, text: filterText, itemSpecId: hasSpecification ? descItem.descItemSpecId : null}));
                this.dispatch(partyDetailClear());

            }
            this.dispatch(registryListFilter({...registryList.filter, text: filterText, itemSpecId: hasSpecification ? descItem.descItemSpecId : null}));
            this.dispatch(registryDetailFetchIfNeeded(value ? value.id : null));
            this.dispatch(modalDialogShow(this, null, <RegistrySelectPage
                titles={[fundName, nodeName, itemName + (hasSpecification ? ': ' + specName : '')]}
                hasParty={hasParty} onSelect={(data) => {
                    onSelect(data);
                    if (hasParty) {
                        this.dispatch(partyListFilter({text:null, type:null, itemSpecId: null}));
                        this.dispatch(partyDetailClear());
                    }
                    this.dispatch(registryListFilter({text: null, registryParentId: null, registryTypeId: null, versionId: null, itemSpecId: null, parents: [], typesToRoot: null}));
                    this.dispatch(registryDetailClear());
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
                return <DescItemLabel onClick={onDetail.bind(this, descItem.record.recordId)} value={value.record} notIdentified={descItem.undefined} />
            } else {
                return <DescItemLabel value={cal ? i18n("subNodeForm.descItemType.calculable") : ""} cal={cal} notIdentified={descItem.undefined} />
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
                    detail={!descItem.undefined && (typePrefix == "output" ? false : !disabled)}
                    onSelectModule={this.handleSelectModule}
                    undefined={descItem.undefined}
                    {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, disabled || descItem.undefined, ['autocomplete-record'])}
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
        const subNode = objectById(node.childNodes, selectedSubNodeId);
        subNode && subNode.name && (nodeName = subNode.name);
    }

    const registryList = storeFromArea(state, AREA_REGISTRY_LIST);
    const partyList = storeFromArea(state, AREA_PARTY_LIST);
    return {
        registryList,
        partyList,
        fundName,
        nodeName
    }
}, null, null, { withRef: true })(DescItemRecordRef);
