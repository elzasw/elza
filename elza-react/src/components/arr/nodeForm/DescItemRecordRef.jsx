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
    registryListFilter,
} from 'actions/registry/registry.jsx';
import classNames from 'classnames';
import {objectById, storeFromArea} from 'shared/utils';
import {MODAL_DIALOG_VARIANT, URL_ENTITY} from '../../../constants';
import {withRouter} from "react-router";
import {goToAe} from "../../../actions/registry/registry";
import { Link } from 'react-router-dom';

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
        const {hasSpecification, descItem, registryList, fund, nodeName, itemName, specName, history} = this.props;
        const oldFilter = {...registryList.filter};

        this.props.dispatch(
            registryListFilter({
                ...registryList.filter,
                registryTypeId: null,
                itemTypeId: this.props.itemTypeId,
                text: filterText,
                itemSpecId: hasSpecification ? descItem.descItemSpecId : null,
                versionId: fund?.versionId,
            }),
        );

        this.props.dispatch(goToAe(history, value ? value.id : null, false, false));
        this.props.dispatch(
            modalDialogShow(
                this,
                null,
                <RegistrySelectPage
                    titles={[fund?.name, nodeName, itemName + (hasSpecification ? ': ' + specName : '')]}
                    fund={fund}
                    onSelect={data => {
                        onSelect(data);
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
            if (record && !descItem.undefined) {
                return (
                    <Link to={`${URL_ENTITY}/${record.id}`}>
                        <DescItemLabel value={record.name} />
                    </Link>
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
                        detail={!descItem.undefined && (typePrefix === 'output' && cal ? false : !disabled)}
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

export default withRouter(connect(
    (state, props) => {
        let nodeName = null;
        let fund = undefined;
        if (props.typePrefix !== 'output' && props.typePrefix !== 'accesspoint' && props.typePrefix !== 'ap-name') {
            const {
                arrRegion: {activeIndex, funds},
            } = state;
            fund = funds[activeIndex];
            const {nodes} = fund;
            const node = nodes.nodes[nodes.activeIndex];
            const {selectedSubNodeId} = node;
            const subNode = objectById(node.childNodes, selectedSubNodeId);
            nodeName = subNode && subNode.name ? subNode.name : nodeName;
        }

        const registryList = storeFromArea(state, AREA_REGISTRY_LIST);
        return {
            registryList,
            fund,
            nodeName,
        };
    },
    null,
    null,
    {forwardRef: true},
)(DescItemRecordRef));
