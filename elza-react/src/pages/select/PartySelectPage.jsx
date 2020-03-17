import React from 'react';
import {connect} from 'react-redux';

import {i18n, Icon, RibbonGroup, RibbonSplit} from 'components/shared';
import {Button} from '../../components/ui';
import {storeFromArea} from 'shared/utils';
import SelectPage from './SelectPage.jsx';
import {AREA_PARTY_DETAIL} from 'actions/party/party.jsx';
import PartyPage from '../party/PartyPage';

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class PartySelectPage extends SelectPage {
    /**
     * @override
     */
    handleConfirm = () => {
        const {partyDetail, onSelect} = this.props;
        if (!partyDetail.fetched || partyDetail.isFetching || !partyDetail.id) {
            return;
        }
        onSelect(partyDetail.data);
    };

    buildRibbonParts() {
        const parts = super.buildRibbonParts();
        parts.primarySection.push(<RibbonSplit key={'ribbon-spliter-pages'} />);
        parts.primarySection.push(
            <RibbonGroup key="ribbon-group-pages" className="large">
                <Button className="active">
                    <Icon glyph="fa-users" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.party')}</span>
                    </div>
                </Button>
            </RibbonGroup>,
        );
        return parts;
    }

    render() {
        const props = this.getPageProps();
        return <PartyPage {...props} />;
    }
}

export default connect(state => {
    const partyDetail = storeFromArea(state, AREA_PARTY_DETAIL);
    return {
        partyDetail,
    };
})(PartySelectPage);
