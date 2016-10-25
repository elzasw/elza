/**
 * Stránka archivních pomůcek.
 */

require('./ArrPage.less');
require('./ArrDataGridPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {FundSettingsForm, Tabs, Icon, FundDataGrid, Ribbon, i18n, ArrFundPanel} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

import ArrParentPage from "./ArrParentPage.jsx";

import {
    BulkActionsDialog,
    RibbonGroup,
    AbstractReactComponent,
    NodeTabs,
    ListBox2,
    LazyListBox,
    VisiblePolicyForm,
    Loading,
    FundPackets,
    FundFiles,
    FundTreeMain
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
import {Utils} from 'components/index.jsx';
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
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {selectTab} from 'actions/global/tab.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'


const keyModifier = Utils.getKeyModifier()
const keymap = ArrParentPage.mergeKeymap({});

const shortcutManager = new ShortcutsManager(keymap)

const ArrDataGridPage = class ArrDataGridPage extends ArrParentPage {
    constructor(props) {
        super(props, "fa-page");
    }

    componentDidMount() {
        super.componentDidMount();
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);
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

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts ArrDataGridPage", '[' + action + ']', this);
        super.handleShortcuts(action);
    }

    renderCenterPanel(readMode, closed) {
        const {packetTypes, descItemTypes, calendarTypes, rulDataTypes, ruleSet, userDetail} = this.props;
        const fund = this.getActiveFund(this.props);

        return (
            <div className="datagrid-content-container">
                <FundDataGrid
                    versionId={fund.versionId}
                    fundId={fund.id}
                    fund={fund}
                    closed={fund.closed}
                    readMode={readMode}
                    fundDataGrid={fund.fundDataGrid}
                    descItemTypes={descItemTypes}
                    packetTypes={packetTypes}
                    calendarTypes={calendarTypes}
                    rulDataTypes={rulDataTypes}
                    ruleSet={ruleSet}
                />
            </div>
        )
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
        packetTypes: refTables.packetTypes,
        ruleSet: refTables.ruleSet,
        tab,
    }
}

ArrDataGridPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    arrRegion: React.PropTypes.object.isRequired,
    developer: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    ruleSet: React.PropTypes.object.isRequired,
}

module.exports = connect(mapStateToProps)(ArrDataGridPage);
