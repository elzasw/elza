import './ArrPage.scss';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import ArrParentPage from './ArrParentPage';
import { Ribbon } from '../../components/index';
import { RibbonGroup, Icon } from '../../components/shared';
import { Button } from '../../components/ui';
import { getFundVersion, urlFundAb, urlFundAipExplorer } from '../../constants';
import AipExplorerTabs from '../../components/aip/explorer/AipExplorerTabs';
import { explorerPageMessages } from '../../components/aip/messages';
import { FormattedMessage } from 'react-intl';
import type { AppState, Fund, UserDetail } from 'typings/store';

/**
 * Průzkumník archivního balíčku v kontextu archivního souboru.
 *
 * Je to běžná stránka archivního souboru, takže nese jeho pás karet; seznam balíčků
 * zůstává na samostatné stránce a slouží k výběru a připojování.
 */
const AREA = "AIP";

/**
 * Props teto stranky. Zakladni trida ArrParentPage je zatim netypovane .jsx,
 * takze je nelze zdedit; popsany je jen rozsah, ktery stranka pouziva.
 */
type ArrAipExplorerPageProps = {
    dispatch: (action: unknown) => unknown;
    userDetail: UserDetail;
    arrRegion: { activeIndex: number | null; funds: Fund[] };
    match: { params: { aipId: string } };
};

class ArrAipExplorerPage extends ArrParentPage {
    area = AREA

    constructor(props: ArrAipExplorerPageProps) {
        super(props, 'fa-page');
    }

    getPageUrl(fund: Fund) {
        return urlFundAipExplorer(fund.id, this.getAipId(), getFundVersion(fund));
    }

    getAipId(): number {
        return Number(this.props.match.params.aipId);
    }

    buildRibbon(readMode: boolean, closed: boolean) {
        const activeFund = this.getActiveFund(this.props);

        const altActions = [
            <Button key="backToAips"
                    onClick={() => this.props.history.push(urlFundAb(activeFund.id, getFundVersion(activeFund)))}>
                <Icon glyph="fa-arrow-left" />
                <div>
                    <span className="btnText"><FormattedMessage {...explorerPageMessages.back}/></span>
                </div>
            </Button>,
        ];

        return (
            <Ribbon
                arr
                subMenu
                fundId={activeFund ? activeFund.id : null}
                versionId={getFundVersion(activeFund)}
                altSection={<RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>}
            />
        );
    }

    hasPageShowRights(userDetail: UserDetail, activeFund: Fund | null) {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    }

    renderCenterPanel(readMode: boolean, closed: boolean) {
        return (
            <div className="aip-explorer-tabs-container">
                <AipExplorerTabs aipId={this.getAipId()}/>
            </div>
        );
    }
}

function mapStateToProps(state: AppState) {
    const {arrRegion, refTables, form, focus, developer, userDetail, tab} = state;
    return {
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        tab,
    };
}

ArrAipExplorerPage.propTypes = {
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrAipExplorerPage);
