import './ArrPage.scss';
import './ArrAipPage.scss';
import PropTypes from 'prop-types';
import {connect} from 'react-redux';
import ArrParentPage from './ArrParentPage';
import {RibbonGroup} from '../../components/shared';
import { Ribbon} from '../../components/index';
import { getFundVersion, urlFundAb, urlFundAipExplorer} from "../../constants";
import AipTable from '../../components/aip/AipTable';
import {selectAip} from '../../actions/aip/aip';
import type { AppState, Fund, UserDetail } from 'typings/store';

import { AipFieldName, AipLinkState, AipProblemType } from 'elza-api';
import { buildFilter } from 'components/aip/filter/aipFilterModel';
import { AipFilterEntry } from 'typings/store';
import ActionsContainer from 'components/arr/aip/ActionsContainer';

/**
 * Stránka archivních balíčků
 */

const AREA = "AIP"

/**
 * Conditions the screen applies itself: this fund, with metadata loaded and no load error.
 */
const initialFilters = (fundId: number): AipFilterEntry[] => [
    {
        id: "fund",
        field: AipFieldName.Fund,
        filter: buildFilter(AipFieldName.Fund, "ref", {operation: "EQ", value: fundId}),
        invisible: true,
    },
    {
        id: "problemType",
        field: AipFieldName.ProblemType,
        filter: buildFilter(AipFieldName.ProblemType, "problemType",
            {operation: "NEQ", value: AipProblemType.MetadataError}),
        invisible: true,
    },
];

/**
 * Props teto stranky. Zakladni trida ArrParentPage je zatim netypovane .jsx,
 * takze je nelze zdedit; popsany je jen rozsah, ktery stranka pouziva.
 */
type ArrAipPageProps = {
    dispatch: (action: unknown) => unknown;
    userDetail: UserDetail;
    arrRegion: { activeIndex: number | null; funds: Fund[] };
};

class ArrAipPage extends ArrParentPage {
    area = AREA

    constructor(props: ArrAipPageProps) {
        super(props, 'fa-page');
    }

    componentDidMount() {
        super.componentDidMount()
        this.resolveUrls()
    }

    UNSAFE_componentWillReceiveProps(nextProps: ArrAipPageProps) {
        super.UNSAFE_componentWillReceiveProps(nextProps);
    }

    getPageUrl(fund: Fund) {
        return urlFundAb(fund.id, getFundVersion(fund));
    }

    handleShortcuts(action: string, e: KeyboardEvent) {
        console.log('#handleShortcuts ArrAipPage', '[' + action + ']', this);
        super.handleShortcuts(action, e);
    }
    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode: boolean, closed: boolean) {
        const activeFund = this.getActiveFund(this.props);

        const altActions: JSX.Element[] = [];

        const itemActions: JSX.Element[] = [];

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return (
            <Ribbon
                arr
                subMenu
                fundId={activeFund ? activeFund.id : null}
                versionId={getFundVersion(activeFund)}
                altSection={altSection}
                itemSection={itemSection}
            />
        );
    }

    hasPageShowRights(userDetail: UserDetail, activeFund: Fund | null) {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    }

    /**
     * Průzkumník je samostatná stránka, seznam proto zabírá celou šířku; obě akce
     * připojení pracují s výběrem v seznamu, ne s tím, co je v průzkumníku vidět.
     */
    renderCenterPanel(readMode: boolean, closed: boolean) {
        const activeFund = this.getActiveFund(this.props);
        return (
            <div className='aip-center-panel'>
                <AipTable
                    onAipSelect={(id) => this.props.dispatch(selectAip(id))}
                    onExplore={(id) => this.props.history.push(
                        urlFundAipExplorer(activeFund.id, id, getFundVersion(activeFund)))}
                    initialFilters={initialFilters(activeFund.id)}
                    hiddenValues={["fund.name", "fundCode", "institution.name", "institutionCode"]}
                />
                <ActionsContainer fund={activeFund} readMode={readMode}/>
            </div>
        );
    }
};

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

ArrAipPage.propTypes = {
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrAipPage);
