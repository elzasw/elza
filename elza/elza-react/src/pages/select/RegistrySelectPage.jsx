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

const OPEN_PAGE = {
    PARTY: 'party',
    REGISTRY: 'registry',
};

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistrySelectPage extends SelectPage {
    state = {
        openPage: 'registry',
    };

    static propTypes = {
        hasParty: PropTypes.bool,
    };

    static defaultProps = {
        hasParty: true,
    };

    handlePageChange = page => {
        if (this.state.openPage !== page) {
            this.setState({openPage: page});
        }
    };

    /**
     * @override
     */
    handleConfirm = () => {
        const {registryDetail, onSelect} = this.props;
        switch (this.state.openPage) {
            case OPEN_PAGE.PARTY:
                alert('Open page Party');
                console.warn('%c ::party ', 'background: black; color: yellow;');
                break;
            case OPEN_PAGE.REGISTRY:
                if (!registryDetail.fetched || registryDetail.isFetching || !registryDetail.id) {
                    return;
                }
                onSelect(registryDetail.data);
                break;
            default:
                break;
        }
        this.handleClose();
    };

    buildRibbonParts() {
        const {openPage} = this.state;
        const {hasParty} = this.props;

        const parts = super.buildRibbonParts();
        parts.primarySection.push(<RibbonSplit key={'ribbon-spliter-pages'}/>);
        const items = [];
        items.push(
            <Button
                key="registry-btn-key-reg"
                className={classNames({active: openPage === OPEN_PAGE.REGISTRY})}
                onClick={this.handlePageChange.bind(this, OPEN_PAGE.REGISTRY)}
            >
                <Icon glyph="fa-th-list"/>
                <div>
                    <span className="btnText">{i18n('ribbon.action.registry')}</span>
                </div>
            </Button>,
        );

        if (hasParty) {
            items.push(
                <Button
                    key={'registry-btn-key-party'}
                    className={classNames({active: openPage === OPEN_PAGE.PARTY})}
                    onClick={this.handlePageChange.bind(this, OPEN_PAGE.PARTY)}
                >
                    <Icon glyph="fa-users"/>
                    <div>
                        <span className="btnText">{i18n('ribbon.action.party')}</span>
                    </div>
                </Button>,
            );
        }

        parts.primarySection.push(
            <RibbonGroup key="ribbon-group-pages" className="large">
                {items}
            </RibbonGroup>,
        );

        return parts;
    }

    getPageProps() {
        let props = {
            ...super.getPageProps(),
            goToPartyPerson: this.handlePageChange.bind(this, OPEN_PAGE.PARTY),
        };

        return props;
    }

    render() {
        const {openPage} = this.state;
        const props = this.getPageProps();

        switch (openPage) {
            case OPEN_PAGE.REGISTRY:
                return <RegistryPage {...props} />;
            default:
                break;
        }
    }
}

export default connect(state => {
    const registryDetail = storeFromArea(state, AREA_REGISTRY_DETAIL);
    return {
        registryDetail,
    };
})(RegistrySelectPage);
