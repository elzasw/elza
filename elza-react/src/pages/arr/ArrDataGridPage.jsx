import './ArrPage.scss';
import './ArrDataGridPage.scss';

/**
 * Stránka archivních pomůcek.
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import * as types from 'actions/constants/ActionTypes.js';
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'

import ArrParentPage from "./ArrParentPage.jsx";
import {Tabs, Icon, i18n, RibbonGroup, AbstractReactComponent, ListBox2, LazyListBox, StoreHorizontalLoader, Utils} from 'components/shared';
import {
    FundDataGrid,
    Ribbon,
    FundSettingsForm,
    ArrFundPanel,
    NodeTabs,
    FundPackets,
    FundFiles,
    FundTreeMain
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import PageLayout from "../shared/layout/PageLayout";
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
import {isFundRootId, getSettings, setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy.jsx'
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {createFundRoot} from 'components/arr/ArrUtils.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {fundTreeFetchIfNeeded} from 'actions/arr/fundTree.jsx'
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import DataGridExportDialog from "../../components/arr/DataGridExportDialog";

const ArrDataGridPage = class ArrDataGridPage extends ArrParentPage {
    constructor(props) {
        super(props, "fa-page");
    }

    componentDidMount() {
        this.props.dispatch(refRuleSetFetchIfNeeded());
        super.componentDidMount();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(refRuleSetFetchIfNeeded());
        super.UNSAFE_componentWillReceiveProps(nextProps);
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const {arrRegion} = this.props;

        const activeFund = this.getActiveFund(this.props);

        var altActions = [];

        var itemActions = [];
        itemActions.push(
            <Button key="export-dataGrid" onClick={() => {this.handleDataGridExport(activeFund.versionId)}}><Icon glyph="fa-download"/>
                <div><span className="btnText">{i18n('ribbon.action.arr.dataGrid.export')}</span></div>
            </Button>
        )

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr subMenu fundId={activeFund ? activeFund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    handleDataGridExport(versionId) {
        const fund = this.getActiveFund(this.props);
        this.props.dispatch(modalDialogShow(this, i18n("dataGrid.export.title"), <DataGridExportDialog versionId={versionId} fundDataGrid={fund.fundDataGrid} />));
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }

    handleShortcuts(action,e) {
        console.log("#handleShortcuts ArrDataGridPage", '[' + action + ']', this);
        super.handleShortcuts(action,e);
    }

    renderCenterPanel(readMode, closed) {
        const {descItemTypes, calendarTypes, rulDataTypes, ruleSet, userDetail} = this.props;
        const fund = this.getActiveFund(this.props);

        return <div className="datagrid-content-container">
            <StoreHorizontalLoader store={ruleSet} />
            {ruleSet.fetched && <FundDataGrid
                versionId={fund.versionId}
                fundId={fund.id}
                fund={fund}
                closed={fund.closed}
                readMode={readMode}
                fundDataGrid={fund.fundDataGrid}
                descItemTypes={descItemTypes}
                calendarTypes={calendarTypes}
                rulDataTypes={rulDataTypes}
                ruleSet={ruleSet}
            />}
        </div>
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        tab,
    }
}

ArrDataGridPage.propTypes = {
    splitter: PropTypes.object.isRequired,
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    calendarTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrDataGridPage);
