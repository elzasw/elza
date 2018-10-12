import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'

import classNames from 'classnames';

import {AbstractReactComponent, i18n, Loading, Icon, RibbonGroup, RibbonMenu, RibbonSplit} from 'components/shared';
import {Button} from 'react-bootstrap';
import {AREA_PARTY_DETAIL} from 'actions/party/party.jsx';
import {AREA_REGISTRY_DETAIL} from 'actions/registry/registry.jsx';
import {storeFromArea} from 'shared/utils'
import SelectPage from './SelectPage.jsx'
import RegistryPage from '../registry/RegistryPage.jsx'
import PartyPage from '../party/PartyPage.jsx'

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
        openPage: 'registry'
    };

    static PropTypes = {
        hasParty: React.PropTypes.bool
    };

    static defaultProps = {
        hasParty: true
    };

    handlePageChange = (page) => {
        if (this.state.openPage !== page) {
            this.setState({openPage: page});
        }
    };

    /**
     * @override
     */
    handleConfirm = () => {
        const {registryDetail, partyDetail, onSelect} = this.props;
        switch (this.state.openPage) {
            case OPEN_PAGE.PARTY:
                if (!partyDetail.fetched || partyDetail.isFetching || !partyDetail.id) {
                    return;
                }
                onSelect(partyDetail.data.accessPoint);
                break;
            case OPEN_PAGE.REGISTRY:
                if (!registryDetail.fetched || registryDetail.isFetching || !registryDetail.id) {
                    return;
                }
                onSelect(registryDetail.data);
                break;
        }
        this.handleClose();
    };

    buildRibbonParts() {
        const {openPage} = this.state;
        const {hasParty} = this.props;


        const parts = super.buildRibbonParts();
        parts.primarySection.push(<RibbonSplit key={"ribbon-spliter-pages"} />);
        const items = [];
        items.push(<Button key="registry-btn-key-reg" className={classNames({active: openPage == OPEN_PAGE.REGISTRY})}
                onClick={this.handlePageChange.bind(this, OPEN_PAGE.REGISTRY)}>
            <Icon glyph="fa-th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div>
        </Button>);

        if (hasParty) {
            items.push(<Button key={"registry-btn-key-party"} className={classNames({active: openPage == OPEN_PAGE.PARTY})} onClick={this.handlePageChange.bind(this, OPEN_PAGE.PARTY)}>
                <Icon glyph="fa-users" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div>
            </Button>);
        }

        parts.primarySection.push(
            <RibbonGroup key="ribbon-group-pages" className="large">
                {items}
            </RibbonGroup>
        );

        return parts;
    };
    getPageProps(){
        let props = {
            ...super.getPageProps(),
            goToPartyPerson: this.handlePageChange.bind(this, OPEN_PAGE.PARTY)
        };

        return props;
    }

    render() {
        const {openPage} = this.state;
        const props = this.getPageProps();

        switch (openPage) {
            case OPEN_PAGE.REGISTRY:
                return <RegistryPage {...props} />;
            case OPEN_PAGE.PARTY:
                return <PartyPage {...props} />;
        }
    }
}


export default connect((state) => {
    const registryDetail = storeFromArea(state, AREA_REGISTRY_DETAIL);
    const partyDetail = storeFromArea(state, AREA_PARTY_DETAIL);
    return {
        registryDetail,
        partyDetail
    }
})(RegistrySelectPage);
