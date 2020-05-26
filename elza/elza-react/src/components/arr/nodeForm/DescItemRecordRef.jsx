import PropTypes from 'prop-types';
import React from 'react';

import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, i18n} from 'components/shared';
import {connect} from 'react-redux';
import {decorateAutocompleteValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import './DescItemRecordRef.scss';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {
    AREA_REGISTRY_LIST,
    registryDetailClear,
    registryDetailFetchIfNeeded,
    registryListFilter,
} from 'actions/registry/registry.jsx';
import classNames from 'classnames';
import {objectById, storeFromArea} from 'shared/utils';
import {MODAL_DIALOG_VARIANT} from '../../../constants.tsx';

//import RegistrySelectPage from 'pages/select/RegistrySelectPage.jsx'
let RegistrySelectPage;
import('pages/select/RegistrySelectPage.jsx').then(a => {
    RegistrySelectPage = a.default;
});

//import RegistryField from "../../registry/RegistryField";
let RegistryField;
import('../../registry/RegistryField').then(a => {
    RegistryField = a.default;
});

class DescItemRecordRef extends AbstractReactComponent {
    focus() {
        this.registryField.focus();
    }

    static defaultProps = {
        hasSpecification: false,
    };

    static propTypes = {
        descItem: PropTypes.object.isRequired,
        hasSpecification: PropTypes.bool,
        itemName: PropTypes.string.isRequired,
        specName: PropTypes.string,
    };

    handleSelectModule = ({onSelect, filterText, value}) => {
        const {hasSpecification, descItem, registryList, fundName, nodeName, itemName, specName} = this.props;
        const oldFilter = {...registryList.filter};
        const open = (hasParty = false) => {
            // if (hasParty) {
            //     this.props.dispatch(
            //         partyListFilter({
            //             ...partyList.filter,
            //             text: filterText,
            //             itemSpecId: hasSpecification ? descItem.descItemSpecId : null,
            //         }),
            //     );
            //     this.props.dispatch(partyDetailClear());
            // }
            console.warn(this.props.itemTypeId);
            this.props.dispatch(
                registryListFilter({
                    ...registryList.filter,
                    registryTypeId: null,
                    itemTypeId: this.props.itemTypeId,
                    text: filterText,
                    itemSpecId: hasSpecification ? descItem.descItemSpecId : null,
                }),
            );

            this.props.dispatch(registryDetailFetchIfNeeded(value ? value.id : null));
            this.props.dispatch(
                modalDialogShow(
                    this,
                    null,
                    <RegistrySelectPage
                        titles={[fundName, nodeName, itemName + (hasSpecification ? ': ' + specName : '')]}
                        hasParty={hasParty}
                        onSelect={data => {
                            onSelect(data);
                            // if (hasParty) {
                            //     this.props.dispatch(
                            //         partyListFilter({
                            //             text: null,
                            //             type: null,
                            //             itemSpecId: null,
                            //         }),
                            //     );
                            //     this.props.dispatch(partyDetailClear());
                            // }
                            this.props.dispatch(registryListFilter({...oldFilter}));
                            this.props.dispatch(registryDetailClear());
                        }}
                    />,
                    classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER),
                    () => {
                        this.props.dispatch(registryListFilter({...oldFilter}));
                    },
                ),
            );
        };

        if (hasSpecification) {
            WebApi.specificationHasParty(descItem.descItemSpecId).then(open);
        } else {
            open();
        }
    };

    render() {
        const {
            descItem,
            locked,
            singleDescItemTypeEdit,
            hasSpecification,
            readMode,
            cal,
            onDetail,
            typePrefix,
            ...otherProps
        } = this.props;
        const record = descItem.record ? descItem.record : null;
        if (readMode) {
            if (record) {
                return (
                    <DescItemLabel
                        onClick={onDetail.bind(this, record.id)}
                        value={record.record}
                        notIdentified={descItem.undefined}
                    />
                );
            } else {
                return (
                    <DescItemLabel
                        value={cal ? i18n('subNodeForm.descItemType.calculable') : ''}
                        cal={cal}
                        notIdentified={descItem.undefined}
                    />
                );
            }
        }

        let disabled = locked;
        if (hasSpecification && !descItem.descItemSpecId) {
            disabled = true;
        }

        return (
            <div className="desc-item-value desc-item-value-parts">
                <ItemTooltipWrapper tooltipTitle="dataType.recordRef.format" className="tooltipWrapper">
                    <RegistryField
                        {...otherProps}
                        ref={ref => (this.registryField = ref)}
                        itemSpecId={descItem.descItemSpecId}
                        value={record}
                        footer={!singleDescItemTypeEdit}
                        footerButtons={false}
                        detail={!descItem.undefined && (typePrefix === 'output' ? false : !disabled)}
                        onSelectModule={this.handleSelectModule}
                        undefined={descItem.undefined}
                        {...decorateAutocompleteValue(
                            this,
                            descItem.hasFocus,
                            descItem.error.value,
                            disabled || descItem.undefined,
                            ['autocomplete-record'],
                        )}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default connect(
    (state, props) => {
        let fundName = null,
            nodeName = null;
        if (props.typePrefix !== 'output' && props.typePrefix !== 'accesspoint' && props.typePrefix !== 'ap-name') {
            const {
                arrRegion: {activeIndex, funds},
            } = state;
            const fund = funds[activeIndex];
            const {nodes} = fund;
            fundName = fund.name;
            const node = nodes.nodes[nodes.activeIndex];
            const {selectedSubNodeId} = node;
            const subNode = objectById(node.childNodes, selectedSubNodeId);
            subNode && subNode.name && (nodeName = subNode.name);
        }

        const registryList = storeFromArea(state, AREA_REGISTRY_LIST);
        return {
            registryList,
            fundName,
            nodeName,
        };
    },
    null,
    null,
    {forwardRef: true},
)(DescItemRecordRef);
