import './ArrPage.scss';
import './ArrAipPage.scss';
import PropTypes from 'prop-types';
import {connect, useSelector} from 'react-redux';
import ArrParentPage from './ArrParentPage';
import {i18n, Icon, RibbonGroup} from '../../components/shared';
import { Ribbon} from '../../components/index';
import { getFundVersion, urlFundAb} from "../../constants";
import AipTable from '../../components/aip/AipTable';
import AipExplorer from '../../components/aip/explorer/AipExplorer';
import { ExplorerMode } from 'components/aip/explorer/ExplorerContext';
import {aipsFetchIfNeeded, AREA_SELECTED_AIPS, selectAip} from '../../actions/aip/aip';
import { generateUUID } from 'components/aip/utils';
import { AipFilterCriteria } from 'components/aip/filter/forms/EnumAipFilterCriteria';
import ActionsContainer from 'components/arr/aip/ActionsContainer';
import {AppState} from "../../typings/store";
import {storeFromArea} from "../../shared/utils";
import {Button} from "react-bootstrap";
import {Api} from "../../api";

/**
 * Stránka archivních balíčků
 */

const ArrAipPage = class ArrAipPage extends ArrParentPage {
    selectedAips = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIPS));

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

    handleLoadMetadata = () => {
        let aipIds = this.selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipCreateDaoStructure(aipIds).then(() => {
            this.props.dispatch(aipsFetchIfNeeded(true))
        });
    }
    handleDeleteMetadata = () => {
        let aipIds = this.selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipDeleteDaoStructure(aipIds).then(() => {
            this.props.dispatch(aipsFetchIfNeeded(true))
        });
    }
    handleLoadAips = () => {
        let aipIds = this.selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipDownloadCompleteAip(aipIds).then(() => {
            this.props.dispatch(aipsFetchIfNeeded(true))
        });
    }
    handleDeleteAips = () => {
        let aipIds = this.selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipDeleteCompleteAip(aipIds).then(() => {
            this.props.dispatch(aipsFetchIfNeeded(true))
        });
    }
    handleUpdateAips = () => {}
    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const activeFund = this.getActiveFund(this.props);

        const altActions = [];

        const itemActions = [];

        if (this.selectedAips?.rows?.length > 0) {
            altActions.push(
                <Button key="metadata" onClick={this.handleLoadMetadata}>
                    <Icon glyph="fa-download" />
                    <div>
                        <span className="btnText">{i18n("aip.actions.metadata")}</span>
                    </div>
                </Button>
            );
            altActions.push(
                <Button key="deleteMetadata" onClick={this.handleDeleteMetadata}>
                    <Icon glyph="fa-trash" />
                    <div>
                        <span className="btnText">{i18n("aip.actions.deleteMetadata")}</span>
                    </div>
                </Button>
            );
            altActions.push(
                <Button key="loadAips" onClick={this.handleLoadAips}>
                    <Icon glyph="fa-cloud-download " />
                    <div>
                        <span className="btnText">{i18n("aip.actions.loadAips")}</span>
                    </div>
                </Button>
            );
            altActions.push(
                <Button key="deleteAips" onClick={this.handleDeleteAips}>
                    <Icon glyph="fa-trash" />
                    <div>
                        <span className="btnText">{i18n("aip.actions.deleteAips")}</span>
                    </div>
                </Button>
            );
            altActions.push(
                <Button key="updateAips" onClick={this.handleUpdateAips}>
                    <Icon glyph="fa-refresh" />
                    <div>
                        <span className="btnText">{i18n("aip.actions.updateAips")}</span>
                    </div>
                </Button>
            );
        }

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
