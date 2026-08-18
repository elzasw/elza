import './ArrPage.scss';
import './ArrAipPage.scss';
import PropTypes from 'prop-types';
import {connect} from 'react-redux';
import ArrParentPage from './ArrParentPage';
import {RibbonGroup} from '../../components/shared';
import { Ribbon} from '../../components/index';
import { getFundVersion, urlFundAb} from "../../constants";
import AipTable from '../../components/aip/AipTable';
import AipExplorer from '../../components/aip/explorer/AipExplorer';
import { ExplorerMode } from 'components/aip/explorer/ExplorerContext';
import {selectAip} from '../../actions/aip/aip';
import type { AppState, Fund, UserDetail } from 'typings/store';

import { AipFieldName } from 'elza-api';
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
        id: "metadataLoad",
        field: AipFieldName.MetadataLoad,
        filter: buildFilter(AipFieldName.MetadataLoad, "bool", {operation: "EQ", value: true}),
        invisible: true,
    },
    {
        id: "metadataError",
        field: AipFieldName.MetadataError,
        filter: buildFilter(AipFieldName.MetadataError, "bool", {operation: "EQ", value: false}),
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

    renderLeftPanel(readMode: boolean, closed: boolean) {
        const activeFund = this.getActiveFund(this.props);

        return (
            <AipTable
                onAipSelect={(id) => this.props.dispatch(selectAip(id))}
                initialFilters={initialFilters(activeFund.id)}
                hiddenValues={["fund.name", "institution.name", "institutionCode"]}
            />
        );
    }

    renderCenterPanel(readMode: boolean, closed: boolean) {
        const activeFund = this.getActiveFund(this.props);
        return (
            <div className='aip-center-panel'>
                <AipExplorer mode={ExplorerMode.VIEW}/>
                <ActionsContainer fund={activeFund} readMode={readMode}/>
            </div>
        );
    }
};

function mapStateToProps(state: AppState) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state;
    return {
        splitter: splitter.splitters[AREA],
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
    splitter: PropTypes.object.isRequired,
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrAipPage);
