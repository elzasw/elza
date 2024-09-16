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
import { generateUUID } from 'components/aip/utils';
import { AipFilterCriteria } from 'components/aip/filter/forms/EnumAipFilterCriteria';
import ActionsContainer from 'components/arr/aip/ActionsContainer';

/**
 * Stránka archivních balíčků
 */

const ArrAipPage = class ArrAipPage extends ArrParentPage {

    constructor(props) {
        super(props, 'fa-page');
    }

    componentDidMount() {
        super.componentDidMount()
        this.resolveUrls()
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        super.UNSAFE_componentWillReceiveProps(nextProps);
    }

    getPageUrl(fund) {
        return urlFundAb(fund.id, getFundVersion(fund));
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrAipPage', '[' + action + ']', this);
        super.handleShortcuts(action, e);
    }
    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const activeFund = this.getActiveFund(this.props);

        const altActions = [];

        const itemActions = [];

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

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    }

    renderLeftPanel(readMode, closed) {
        const activeFund = this.getActiveFund(this.props);

        return (
            <AipTable
                onAipSelect={(id) => this.props.dispatch(selectAip(id))}
                initialFilters={[{
                    id: generateUUID(),
                    attr: "fund.name",
                    criteria: AipFilterCriteria.EQUALS,
                    value: activeFund.id,
                    path: "arr_fund",
                    invisible: true,
                }]}
                hiddenValues={["fund.name", "institution.name", "institutionCode"]}
            />
        );
    }

    renderCenterPanel(readMode, closed) {
        return (
            <AipExplorer mode={ExplorerMode.VIEW}/>
        );
    }
    renderRightPanel(readMode, closed) {
        const activeFund = this.getActiveFund(this.props);
        return (
            <ActionsContainer fund={activeFund} readMode={readMode}/>
        );
    }
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state;
    return {
        splitter,
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
