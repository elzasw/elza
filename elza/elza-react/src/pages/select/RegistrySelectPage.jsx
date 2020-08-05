import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import classNames from 'classnames';
import {i18n, Icon, RibbonGroup, RibbonSplit} from 'components/shared';
import {Button} from '../../components/ui';
import {AREA_REGISTRY_DETAIL} from 'actions/registry/registry.jsx';
import {storeFromArea} from 'shared/utils';
import SelectPage from './SelectPage.jsx';
import RegistryPage from '../registry/RegistryPage.jsx';

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistrySelectPage extends SelectPage {
    state = {
    };

    static propTypes = {
    };

    static defaultProps = {
    };

    /**
     * @override
     */
    handleConfirm = () => {
        const {registryDetail, onSelect} = this.props;
        if (!registryDetail.fetched || registryDetail.isFetching || !registryDetail.id) {
            return;
        }
        onSelect(registryDetail.data);
        this.handleClose();
    };

    buildRibbonParts() {
        const parts = super.buildRibbonParts();
        parts.primarySection.push(<RibbonSplit key={'ribbon-spliter-pages'}/>);
        const items = [];
        items.push(
            <Button
                key="registry-btn-key-reg"
                className={classNames({active: true})}
            >
                <Icon glyph="fa-th-list"/>
                <div>
                    <span className="btnText">{i18n('ribbon.action.registry')}</span>
                </div>
            </Button>,
        );

        parts.primarySection.push(
            <RibbonGroup key="ribbon-group-pages" className="large">
                {items}
            </RibbonGroup>,
        );

        return parts;
    }

    render() {
        const props = this.getPageProps();

        return <RegistryPage {...props} />;
    }
}

export default connect(state => {
    const registryDetail = storeFromArea(state, AREA_REGISTRY_DETAIL);
    return {
        registryDetail,
    };
})(RegistrySelectPage);
