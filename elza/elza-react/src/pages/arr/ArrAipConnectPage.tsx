import './ArrPage.scss';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import ArrParentPage from './ArrParentPage';
import { Ribbon } from '../../components/index';
import { RibbonGroup, Icon } from '../../components/shared';
import { Button } from '../../components/ui';
import { getFundVersion, urlFundAb, urlFundAipConnect } from '../../constants';
import AipBulkConnectPanel from '../../components/arr/aip/assignment/AipBulkConnectPanel';
import { explorerPageMessages } from '../../components/aip/messages';
import { FormattedMessage } from 'react-intl';
import type { AppState, Fund, UserDetail } from 'typings/store';

const AREA = "AIP";

/**
 * Props teto stranky. Zakladni trida ArrParentPage je zatim netypovane .jsx,
 * takze je nelze zdedit; popsany je jen rozsah, ktery stranka pouziva.
 */
type ArrAipConnectPageProps = {
    dispatch: (action: unknown) => unknown;
    userDetail: UserDetail;
    arrRegion: { activeIndex: number | null; funds: Fund[] };
    location: { search: string };
};

/** Balíčky z adresy; neplatné hodnoty se vynechají. */
export const parseAipIds = (search: string): number[] =>
    (new URLSearchParams(search).get('aips') ?? '')
        .split(',')
        .map(Number)
        .filter(id => Number.isInteger(id) && id > 0);

/**
 * Hromadné připojení archivních balíčků k popisu archivního souboru.
 *
 * Balíčky nese adresa (?aips=1,2,3), takže stránka přežije znovunačtení a lze se na ni vrátit
 * zpět v prohlížeči; seznam balíčků ji otevírá s vybranými, případně zobrazenými balíčky.
 */
class ArrAipConnectPage extends ArrParentPage {
    area = AREA

    constructor(props: ArrAipConnectPageProps) {
        super(props, 'fa-page');
    }

    getAipIds(): number[] {
        return parseAipIds(this.props.location?.search ?? '');
    }

    getPageUrl(fund: Fund) {
        return urlFundAipConnect(fund.id, this.getAipIds(), getFundVersion(fund));
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
        return <AipBulkConnectPanel aipIds={this.getAipIds()} readOnly={readMode || closed}/>;
    }
}

function mapStateToProps(state: AppState) {
    const {arrRegion, refTables, focus, developer, userDetail, tab} = state;
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

ArrAipConnectPage.propTypes = {
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrAipConnectPage);
